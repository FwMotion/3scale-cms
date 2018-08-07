require_relative '../../local_files'
require_relative '../../mapper'
require 'fakefs/safe'

file1 = 'file1'
file2 = 'file2'

page_name = 'apage'
page = page_name + Threescale::CMS::Mapper::TEMPLATE_SUFFIX
layout_name = 'layout'
layout = Threescale::CMS::Mapper::LAYOUT_PREFIX + layout_name + Threescale::CMS::Mapper::TEMPLATE_SUFFIX
layout_path = layout # is in root folder
file_list = [page, layout]
IGNORE_FILE = Threescale::CMS::LocalFiles::CMS_IGNORE_FILENAME
WORKING_DIR = '/tmp'

shared_examples_for 'NoIgnoredFiles' do
  describe '#list' do
    it "should include #{page}" do
      expect(@subject.list.include? page).to be true
    end

    it "should include #{layout}" do
      expect(@subject.list.include? layout).to be true
    end
  end

  describe '#in_list' do
    it 'should not ignore any file' do
      expect(@subject.in_list page).to be true
    end
  end

  describe '#in_list' do
    it 'should not ignore any file' do
      expect(@subject.in_list layout).to be true
    end
  end

  describe '#directory_entries' do
    it 'should not ignore any file' do
      expect(@subject.directory_entries '.').to match_array(file_list)
    end
  end
end

describe Threescale::CMS::LocalFiles, fakefs: true do
  before(:each) do
    # There is a problem with paths in fakefs if working from '/' https://github.com/fakefs/fakefs/issues/339
    Dir.mkdir WORKING_DIR
    Dir.chdir WORKING_DIR

    # Make a directory with a cmsignore_example file in it, mocking the way the example ignore file is
    # distributed with the source in the gem
    FileUtils.mkdir_p File.dirname(__FILE__)
    cms_example_filename = File.join(File.dirname(__FILE__), '../../', Threescale::CMS::LocalFiles::CMS_IGNORE_EXAMPLE_FILENAME)
    FileUtils.touch cms_example_filename

    @subject = Threescale::CMS::LocalFiles.new Dir.pwd
  end

  context 'There are no local files' do
    describe '#list' do
      it 'should only find the CWD' do
        # noinspection RubyResolve
        expect(@subject.list.size).to eq 1
      end
    end

    describe '#default_layout' do
      it 'should return nil' do
        expect(@subject.get_default_layout).to be nil
      end
    end

    describe '#in_list' do
      it 'should not return true for any file' do
        expect(@subject.in_list 'spec').to be false
      end
    end

    describe '#info' do
      it 'should only list CWD' do
        expect { @subject.info 'details' }.to output(/1 \(non-ignored\) local files/).to_stdout
      end

      it 'should create a .cmsignore file from the example file in the gem' do
        expect(File.exists? '.cmsignore').to eq false
        @subject.info
        expect(File.exists? '.cmsignore').to eq true
      end
    end
  end

  context 'There are local files' do
    def populate_local_files(file_list)
      file_list.each do |file|
        FileUtils.touch(file)
      end
    end

    def populate_ignore_file(ignored_files)
      ignore_file = File.open(IGNORE_FILE, 'w')
      ignored_files.each do |ignored_file|
        ignore_file << ignored_file
      end
      ignore_file.close
    end

    before(:each) do
      populate_local_files file_list
      @subject = Threescale::CMS::LocalFiles.new Dir.pwd
    end

    context 'and there is no ignore file' do
      # noinspection RubyResolve
      it_behaves_like 'NoIgnoredFiles'
    end

    context 'and there is an empty ignore file' do
      before(:each) do
        populate_ignore_file []
      end

      # noinspection RubyResolve
      it_behaves_like 'NoIgnoredFiles'
    end

    context "and there is an ignore file that ignores '#{page}'" do
      ignored_file = page
      non_ignored_file = layout

      before(:each) do
        populate_ignore_file [ignored_file]
      end

      describe '#list' do
        it "should load ignore list from '#{IGNORE_FILE}' and filter out '#{page}'" do
          expect(@subject.list.include? ignored_file).to be false
        end

        it "should load ignore list from '#{IGNORE_FILE}' and still include '#{layout}'" do
          expect(@subject.in_list non_ignored_file).to be true
        end
      end

      describe '#default_layout' do
        it 'should return path to the first layout file found' do
          expect(@subject.get_default_layout).to eq layout_path
        end
      end

      describe '#in_list' do
        it "should load ignore list from '#{IGNORE_FILE}' and filter out '#{page}'" do
          expect(@subject.in_list ignored_file).to be false
        end

        it "should load ignore list from '#{IGNORE_FILE}' and still include '#{layout}'" do
          expect(@subject.in_list non_ignored_file).to be true
        end
      end

      describe '#directory_entries' do
        it "should load ignore list from '#{IGNORE_FILE}' and filter out '#{page}' but retain '#{layout}'" do
          expect(@subject.directory_entries '.').to match_array([non_ignored_file])
        end
      end

      describe '#info' do
        it "should list ''#{page}''" do
          expect { @subject.info 'details' }.to output(/page/).to_stdout
        end

        it "should list ''#{layout}''" do
          expect { @subject.info 'details' }.to output(/layout/).to_stdout
        end
      end

      describe '#save_template' do
        file_contents = 'file contents'
        file_name = 'new_file.tmp'
        directory_name = 'new_dir'
        with_directory_file_name = "#{directory_name}/#{file_name}"
        updated_at = Time.new.to_datetime << 1

        it 'creates file at correct location in CWD' do
          @subject.save_template(file_name, file_contents, updated_at)
          expect(File.exist? file_name).to be true
        end

        it 'creates new directories as required' do
          @subject.save_template(with_directory_file_name, file_contents, updated_at)
          expect(File.exist? directory_name).to be true
        end

        it 'creates new directories as a directory' do
          @subject.save_template(with_directory_file_name, file_contents, updated_at)
          expect(File.directory? directory_name).to be true
        end

        it 'creates file inside specified directory' do
          @subject.save_template(with_directory_file_name, file_contents, updated_at)
          expect(File.exist? with_directory_file_name).to be true
        end

        it 'creates files with the correct content' do
          @subject.save_template(with_directory_file_name, file_contents, updated_at)
          expect(File.read with_directory_file_name).to eq file_contents
        end

        it 'creates the file with the correct timestamp' do
          @subject.save_template(with_directory_file_name, file_contents, updated_at)
          expect(File.mtime with_directory_file_name).to eq updated_at
        end

        it "doesn't modify the timestamp of the parent directory" do
          directory_timestamp = Time.new.to_datetime << 2
          FileUtils.mkdir_p directory_name
          FileUtils.touch directory_name, :mtime => directory_timestamp
          @subject.save_template(with_directory_file_name, file_contents, updated_at)
          expect(File.mtime directory_name).to eq directory_timestamp
        end

        it 'warns you if you save a file that is in the ignore file list' do
          expect { @subject.save_template page, file_contents, updated_at }.to output(/ignored files list/).to_stdout
        end
      end

      describe '#save_section' do
        directory_name = 'new_dir'
        sub_directory_name = 'sub_dir'
        sub_directory_path = "#{directory_name}/#{sub_directory_name}"
        updated_at = Time.new.to_datetime << 1

        it 'creates section as a directory in CWD' do
          @subject.save_section directory_name, updated_at
          expect(File.directory? directory_name).to be true
        end

        it 'creates directories along the path as required' do
          @subject.save_section sub_directory_path, updated_at
          expect(File.directory? directory_name).to be true
        end

        it 'creates section inside new directory' do
          @subject.save_section(sub_directory_path, updated_at)
          expect(File.directory? sub_directory_path).to be true
        end

        it 'creates the file with the correct timestamp' do
          @subject.save_section(sub_directory_path, updated_at)
          expect(File.mtime sub_directory_path).to eq updated_at
        end

        it "doesn't modify the timestamp of the parent directory" do
          directory_timestamp = Time.new.to_datetime << 2
          FileUtils.mkdir_p(directory_name)
          FileUtils.touch directory_name, :mtime => directory_timestamp
          @subject.save_section(sub_directory_path, updated_at)
          expect(File.mtime directory_name).to eq directory_timestamp
        end
      end

      describe '#fetch_and_save_file' do
        updated_at = Time.new.to_datetime << 1

        before(:each) do
          stub_request(:get, 'http://fake.com/').to_return(:status => 200, :body => 'file-content', :headers => {})
        end

        it 'saves downloaded content in specified file' do
          @subject.fetch_and_save_file 'net_file', URI('http://fake.com'), Time.now
          expect(File.exist? 'net_file').to be true
        end

        it 'saves downloaded content over https in specified file' do
          stub_request(:get, 'https://fake.com/').to_return(:status => 200, :body => 'file-content', :headers => {})
          @subject.fetch_and_save_file 'net_file', URI('https://fake.com'), Time.now
          expect(File.exist? 'net_file').to be true
        end

        it 'downloaded content is correct' do
          @subject.fetch_and_save_file 'net_file', URI('http://fake.com'), Time.now
          expect(File.read 'net_file').to eq 'file-content'
        end

        it 'timestamp is set correctly' do
          @subject.fetch_and_save_file 'net_file', URI('http://fake.com'), updated_at
          expect(File.mtime 'net_file').to eq updated_at
        end

        it 'creates directories along the path as required' do
          @subject.fetch_and_save_file 'new_dir/net_file', URI('http://fake.com'), updated_at
          expect(File.directory? 'new_dir').to be true
        end

        it 'creates file inside new directory' do
          @subject.fetch_and_save_file 'new_dir/net_file', URI('http://fake.com'), updated_at
          expect(File.exist? 'new_dir/net_file').to be true
        end

        it "doesn't modify the timestamp of the parent directory" do
          directory_timestamp = Time.new.to_datetime << 2
          directory_name = 'new_dir'
          FileUtils.mkdir_p(directory_name)
          FileUtils.touch directory_name, :mtime => directory_timestamp

          @subject.fetch_and_save_file 'new_dir/net_file', URI('http://fake.com'), updated_at

          expect(File.mtime directory_name).to eq directory_timestamp
        end
      end

      describe '#needs_update' do
        now = Time.new.to_datetime

        it 'need updating if timestamp is newer' do
          FileUtils.touch layout, :mtime => now << 1
          expect(@subject.needs_update layout, now).to be true
        end

        it "doesn't need updating if the timestamp is equal" do
          FileUtils.touch layout, :mtime => now
          expect(@subject.needs_update layout, now).to be false
        end

        it "doesn't need updating if the timestamp is older" do
          FileUtils.touch layout, :mtime => now
          expect(@subject.needs_update layout, now << 1).to be false
        end

        it "doesnt need updating if the files doesn't exist" do
          expect(@subject.needs_update 'file3', now).to be false
        end

        it 'doesnt need update if on the ignore list' do
          FileUtils.touch page, :mtime => now << 1
          expect(@subject.needs_update page, now).to be false
        end
      end

      describe '#needs_creation' do
        it "is true of file doesn't exist" do
          expect(@subject.needs_creation 'file3').to be true
        end

        it 'is false if file exists' do
          expect(@subject.needs_creation layout).to be false
        end
      end

      describe '#is_newer' do
        now = Time.new.to_datetime

        it 'is true if timestamp is older' do
          FileUtils.touch layout, :mtime => now
          expect(@subject.is_newer layout, now << 1).to be true
        end

        it 'is false if the timestamp is equal' do
          FileUtils.touch layout, :mtime => now
          expect(@subject.is_newer layout, now).to be false
        end

        it 'is false updating if the timestamp is newer' do
          FileUtils.touch layout, :mtime => now
          expect(@subject.is_newer layout, now >> 1).to be false
        end

        it "is false if the files doesn't exist" do
          expect(@subject.is_newer 'file3', now).to be false
        end

        it 'is false if on the ignore list' do
          FileUtils.touch page, :mtime => now << 1
          expect(@subject.is_newer page, now).to be false
        end
      end

      describe '#info' do
        it 'should say correct number of ignored files' do
          expect { @subject.info 'details' }.to output(/1 ignored local files/).to_stdout
        end

        it 'should say correct number of non-ignored files' do
          expect { @subject.info 'details' }.to output(/2 \(non-ignored\) local files/).to_stdout
        end

        it "ignored file list should include #{page}" do
          expect { @subject.info 'details' }.to output(/ignored local files\n\t'apage.html.liquid'/).to_stdout
        end

        it "non-ignored file list should include './'" do
          expect { @subject.info 'details' }.to output(/local files.*'.\/'/m).to_stdout
        end

        it "non-ignored file list should include #{layout}" do
          expect { @subject.info 'details' }.to output(/local files.*'l_layout.html.liquid'/m).to_stdout
        end
      end

      describe '#update' do
        today = Time.new.to_datetime
        yesterday = today << 1
        before(:each) do
          FileUtils.touch non_ignored_file, :mtime => yesterday
        end

        it 'should modify the mtime of the file updated' do
          @subject.update non_ignored_file, today
          expect(File.mtime non_ignored_file).to eq today
        end

        it 'should warn you if you are updating an ignored file' do
          expect { @subject.update(ignored_file, today) }.to output(/in the ignored files list/).to_stdout
        end
      end
    end

    context 'there is a file to be ignored in a sub-directory' do
      subdirectory = 'subdir'
      ignored_file = subdirectory + '/' + file1
      non_ignored_file = subdirectory + '/' + file2

      before(:each) do
        FileUtils.mkdir_p subdirectory
        FileUtils.touch ignored_file
        FileUtils.touch non_ignored_file
        populate_ignore_file [ignored_file]
      end

      describe '#list' do
        it 'should filter out ignored file' do
          expect(@subject.list.include? ignored_file).to be false
        end

        it 'should still include non-ignored files' do
          expect(@subject.in_list non_ignored_file).to be true
        end
      end

      describe '#directory_entries' do
        it 'should filter out ignored files but retain non-ignored files' do
          expect(@subject.directory_entries 'subdir').to match_array([non_ignored_file])
        end
      end
    end
  end
end
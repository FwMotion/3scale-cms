require_relative '../../cms'
require_relative '../../mapper'
require_relative 'cms_helper'
require_relative 'local_file_helper'

describe Threescale::CMS::Cms, fakefs: true do
  before(:each) do
    @local_files = make_local_files
  end

  context 'No local files' do
    before(:each) do
      @cms_api = make_cms BASE_CMS
      @subject = Threescale::CMS::Cms.new(@cms_api, @local_files)
    end

    it 'should not upload anything' do
      @subject.upload '.'
    end

    it 'should not fail or attempt to upload a non-existant file' do
      allow(@local_files).to receive(:in_list).with('fake_file').and_return(false)
      @subject.upload 'fake_file'
    end
  end


  context 'there is an index page to upload' do
    index_page_name = 'index'
    index_page_filename = index_page_name + Threescale::CMS::Mapper::TEMPLATE_SUFFIX
    index_path = '/'

    before(:each) do
      make_files @local_files, '.', [index_page_filename]
    end

    context 'CMS without index' do
      before(:each) do
        @cms_api = make_cms NO_INDEX_CMS
        @subject = Threescale::CMS::Cms.new(@cms_api, @local_files)
      end

      describe '#upload' do
        it 'should create index template in the CMS when invoked by filename' do
          expect(@cms_api).to receive(:template_create).with(index_path, 'root', anything, anything, anything, anything, anything, index_page_filename, 1).and_return(['4', Time.now.to_s])
          @subject.upload index_page_filename, nil
        end

        it 'should create index template in the CMS when invoked by root folder name' do
          expect(@cms_api).to receive(:template_create).with(index_path, 'root', anything, anything, anything, anything, anything, index_page_filename, 1).and_return(['4', Time.now.to_s])
          @subject.upload '.', nil
        end
      end
    end

    context 'CMS with index' do
      before(:each) do
        @cms_api = make_cms BASE_CMS
        @subject = Threescale::CMS::Cms.new(@cms_api, @local_files)
      end

      describe '#upload' do
        it 'should update index template in the CMS when invoked by filename' do
          expect(@cms_api).to receive(:template_update).with('123', 'page', index_page_filename).and_return(['123', Time.now.to_s])
          expect(@local_files).to receive(:is_newer).with(anything, anything).and_return(true);
          @subject.upload index_page_filename, nil
        end

        it 'should update index template in the CMS when invoked by root folder name' do
          expect(@local_files).to receive(:is_newer).with(index_page_filename, anything).and_return(true);
          expect(@cms_api).to receive(:template_update).with('123', 'page', index_page_filename).and_return(['123', Time.now.to_s])
          @subject.upload '.', nil
        end
      end
    end
  end

  context 'there is a page to upload' do
    page_name = 'page'
    page_filename = page_name + Threescale::CMS::Mapper::TEMPLATE_SUFFIX

    before(:each) do
      make_files @local_files, '.', [page_filename]
    end

    context 'CMS has no layouts' do
      before(:each) do
        @cms_api = make_cms NO_LAYOUTS_CMS
        @subject = Threescale::CMS::Cms.new(@cms_api, @local_files)
      end

      describe '#upload' do
        it 'should output an error if no local layout is found when uploading a page' do
          allow(@local_files).to receive(:get_default_layout).and_return(nil)
          expect{@subject.upload page_filename, nil}.to output(/Could not find a default layout/).to_stdout
        end
      end
    end

    context 'normal CMS' do
      before(:each) do
        @cms_api = make_cms BASE_CMS
        @subject = Threescale::CMS::Cms.new(@cms_api, @local_files)
      end

      describe '#upload' do
        it 'should create file in the CMS when invoked by name' do
          expect(@cms_api).to receive(:template_create).with("/#{page_name}", 'root', anything, anything, anything, anything, anything, page_filename, 1).and_return(['4', Time.now.to_s])
          @subject.upload page_filename
        end

        it 'should create file in the CMS when invoked without default template type parameter' do
          expect(@cms_api).to receive(:template_create).with("/#{page_name}", 'root', anything, anything, anything, anything, anything, page_filename, 1).and_return(['4', Time.now.to_s])
          @subject.upload page_filename
        end

        it "should create a page in the CMS when invoked using '.'" do
          expect(@cms_api).to receive(:template_create).with("/#{page_name}", 'root', anything, anything, anything, anything, anything, page_filename, 1).and_return(['4', Time.now.to_s])
          @subject.upload('.')
        end

        it 'should use the base filename as the title' do
          expect(@cms_api).to receive(:template_create).with("/#{page_name}", 'root', anything, anything, page_name, anything, anything, page_filename, 1).and_return(['4', Time.now.to_s])
          @subject.upload page_filename, nil
        end

        it 'should use the base filename as the system_name' do
          expect(@cms_api).to receive(:template_create).with("/#{page_name}", 'root', anything, page_name, anything, anything, anything, page_filename, 1).and_return(['4', Time.now.to_s])
          @subject.upload page_filename
        end
      end
    end
  end

  context 'local files and a directory with a file in it' do
    directory_name = 'directory'
    sub_file_name = 'sub_file'
    sub_file_path = "#{directory_name}/#{sub_file_name}"

    before(:each) do
      make_directory @local_files, '.', directory_name, [], [sub_file_name]
      @cms_api = make_cms NO_LAYOUTS_CMS
      @subject = Threescale::CMS::Cms.new(@cms_api, @local_files)
    end

    it 'should create the required section if we upload a file in a sub-directory' do
      expect(@cms_api).to receive(:section_create).with("/#{directory_name}", anything, anything).and_return(['4', Time.now.to_s])
      allow(@cms_api).to receive(:file_create).with("/#{sub_file_path}", anything, sub_file_path, anything).and_return(['5', Time.now.to_s])
      @subject.upload directory_name
    end

    it 'should create the file inside the created section if we upload a file in a sub-directory' do
      allow(@cms_api).to receive(:section_create).with("/#{directory_name}", anything, anything).and_return(['4', Time.now.to_s])
      expect(@cms_api).to receive(:file_create).with("/#{sub_file_path}", anything, sub_file_path, anything).and_return(['5', Time.now.to_s])
      @subject.upload directory_name
    end
  end

  context 'local templates exist, layouts after pages' do
    page_name = 'apage'
    page_filename = page_name + Threescale::CMS::Mapper::TEMPLATE_SUFFIX
    layout_name = 'zzzz'
    layout_filename = Threescale::CMS::Mapper::LAYOUT_PREFIX + layout_name + Threescale::CMS::Mapper::TEMPLATE_SUFFIX
    layout_path = layout_filename # is in the root folder

    before(:each) do
      make_files @local_files, '.', [page_filename, layout_filename]
    end

    context 'CMS has no layouts' do
      before(:each) do
        @cms_api = make_cms NO_LAYOUTS_CMS
        @subject = Threescale::CMS::Cms.new(@cms_api, @local_files)
      end

      describe '#upload' do
        it 'should upload a default layout before pages that need it', fakefs: true do
          allow(@local_files).to receive(:get_default_layout).and_return(layout_path)

          # Expect to upload layout first
          expect(@cms_api).to receive(:template_create).with("/#{layout_name}", 'root', anything, anything, anything, anything, anything, layout_filename, 1).and_return(['4', Time.now.to_s])
          # Expect to then upload the page using the previously uploaded layout as its layout
          expect(@cms_api).to receive(:template_create).with("/#{page_name}", 'root', anything, anything, anything, layout_name, anything, page_filename, 1).and_return(['4', Time.now.to_s])
          @subject.upload '.'
        end
      end
    end
  end

  context 'Set of local files not in the CMS' do
    file_filename = 'afile.jpg'
    liquid_page_name = 'apage'
    liquid_page_filename = liquid_page_name + Threescale::CMS::Mapper::TEMPLATE_SUFFIX
    non_liquid_page_name = 'a_non_liquid_page'
    non_liquid_page_filename = non_liquid_page_name + Threescale::CMS::Mapper::HTML_SUFFIX
    layout_name = 'layout'
    layout_filename = Threescale::CMS::Mapper::LAYOUT_PREFIX + layout_name + Threescale::CMS::Mapper::TEMPLATE_SUFFIX
    partial_name = 'new_partial'
    partial_filename = Threescale::CMS::Mapper::PARTIAL_PREFIX + partial_name + Threescale::CMS::Mapper::TEMPLATE_SUFFIX

    empty_folder = 'empty_folder'
    folder2 = 'folder2'
    empty_subfolder = 'empty_subfolder'
    subfolder_with_contents = 'subfolder_with_contents'

    # TODO builtin page (future)
    # TODO builtin partial (future)

    before(:each) do
      @cms_api = make_cms BASE_CMS # FIXME warnings in base cms due to implicit folder are distracting in rspec output
      @subject = Threescale::CMS::Cms.new(@cms_api, @local_files)
    end

    describe '#upload' do
      it 'should upload a file in root' do
        make_file @local_files, file_filename
        directory_entries @local_files, '.', [file_filename]
        expect(@cms_api).to receive(:file_create).with("/#{file_filename}", anything, file_filename, anything).and_return(['4', Time.now.to_s])
        @subject.upload '.'
      end

      it 'should upload a liquid page in root, appending correct suffix' do
        make_file @local_files, liquid_page_filename
        directory_entries @local_files, '.', [liquid_page_filename]
        expect(@cms_api).to receive(:template_create).with("/#{liquid_page_name}", 'root', anything, anything, anything, anything, 'page', liquid_page_filename, 1).and_return(['5', Time.now.to_s])
        @subject.upload '.'
      end

      it 'should upload a non-liquid page in root, appending correct suffix' do
        make_file @local_files, non_liquid_page_filename
        directory_entries @local_files, '.', [non_liquid_page_filename]
        expect(@cms_api).to receive(:template_create).with("/#{non_liquid_page_name}", 'root', anything, anything, anything, anything, 'page', non_liquid_page_filename, 0).and_return(['5', Time.now.to_s])
        @subject.upload '.'
      end

      it 'should upload a layout in root' do
        make_file @local_files, layout_filename
        directory_entries @local_files, '.', [layout_filename]
        expect(@cms_api).to receive(:template_create).with("/#{layout_name}", 'root', anything, anything, anything, anything, 'layout', layout_filename, 1).and_return(['6', Time.now.to_s])
        @subject.upload '.'
      end

      it 'should upload a partial in root' do
        directory_entries @local_files, '.', [partial_filename]
        make_file @local_files, partial_filename
        expect(@cms_api).to receive(:template_create).with("/#{partial_name}", 'root', anything, anything, anything, anything, 'partial', partial_filename, 1).and_return(['7', Time.now.to_s])
        @subject.upload '.'
      end

      it 'should upload an empty folder' do
        make_directory @local_files, '.', empty_folder, [], []
        expect(@cms_api).to receive(:section_create).with("/#{empty_folder}", anything, '1').and_return(['4', Time.now.to_s])
        @subject.upload '.'
      end

      it 'should upload a folder with contents' do
        make_directory @local_files, '.', folder2, [], [file_filename]
        expect(@cms_api).to receive(:section_create).with("/#{folder2}", anything, '1').and_return(['4', Time.now.to_s])
        expect(@cms_api).to receive(:file_create).with("/#{folder2}/#{file_filename}", anything, "#{folder2}/#{file_filename}", anything).and_return(['4', Time.now.to_s])
        @subject.upload '.'
      end

      it 'should upload an empty subfolder inside a folder' do
        make_directory @local_files, '.', folder2, [empty_subfolder], []
        expect(@cms_api).to receive(:section_create).with("/#{folder2}", anything, anything).and_return(['4', Time.now.to_s]).and_return(['4', Time.now.to_s])
        expect(@cms_api).to receive(:section_create).with("/#{folder2}/#{empty_subfolder}", anything, anything).and_return(['4', Time.now.to_s])
        @subject.upload '.'
      end

      it 'should upload a subfolder with contents inside a folder' do
        make_directory @local_files, '.', folder2, [subfolder_with_contents], []
        make_directory @local_files, folder2, subfolder_with_contents, [], [file_filename, liquid_page_filename]
        expect(@cms_api).to receive(:section_create).with("/#{folder2}", anything, anything).and_return(['4', Time.now.to_s])
        expect(@cms_api).to receive(:section_create).with("/#{folder2}/#{subfolder_with_contents}", anything, anything).and_return(['5', Time.now.to_s])
        @subject.upload '.'
      end

      it 'should upload a file in a folder as a file' do
        make_directory @local_files, '.', folder2, [], [file_filename]
        expect(@cms_api).to receive(:section_create).with("/#{folder2}", anything, anything).and_return(['3', Time.now.to_s])
        expect(@cms_api).to receive(:file_create).with("/#{folder2}/#{file_filename}", anything, "#{folder2}/#{file_filename}", anything).and_return(['4', Time.now.to_s])
        @subject.upload '.'
      end

      it 'should upload a page in a folder as a page' do
        make_directory @local_files, '.', folder2, [], [liquid_page_filename]
        expect(@cms_api).to receive(:section_create).with("/#{folder2}", anything, anything).and_return(['3', Time.now.to_s])
        expect(@cms_api).to receive(:template_create).with("/#{folder2}/#{liquid_page_name}", folder2, '3', "#{folder2}/#{liquid_page_name}", liquid_page_name, MAIN_LAYOUT_NAME, 'page', "#{folder2}/#{liquid_page_filename}", 1).and_return(['4', Time.now.to_s])
        @subject.upload '.'
      end

      it 'should upload a layout in a folder as a layout' do
        make_directory @local_files, '.', folder2, [], [layout_filename]
        expect(@cms_api).to receive(:section_create).with("/#{folder2}", anything, anything).and_return(['3', Time.now.to_s])
        expect(@cms_api).to receive(:template_create).with("/#{folder2}/#{layout_name}", folder2, '3', "#{folder2}/#{layout_name}", layout_name, anything, 'layout', "#{folder2}/#{layout_filename}", 1).and_return(['4', Time.now.to_s])
        @subject.upload '.'
      end

      it 'should upload a partial in a folder as a partial' do
        make_directory @local_files, '.', folder2, [], [partial_filename]
        expect(@cms_api).to receive(:section_create).with("/#{folder2}", anything, anything).and_return(['3', Time.now.to_s])
        expect(@cms_api).to receive(:template_create).with("/#{folder2}/#{partial_name}", folder2, '3', "#{folder2}/#{partial_name}", partial_name, anything, 'partial', "#{folder2}/#{partial_filename}", 1).and_return(['4', Time.now.to_s])
        @subject.upload '.'
      end

      it 'should upload an empty subfolder of a folder in root' do
        make_directory @local_files, '.', empty_subfolder, [], []
        expect(@cms_api).to receive(:section_create).with("/#{empty_subfolder}", anything, anything).and_return(['3', Time.now.to_s])
        @subject.upload '.'
      end

      # with the name of a folder in root (name not an implicit folder)
      # folder is empty --> create section only
      # folder has files --> create section first and then the file in that section
      # folder has files (section already exists) --> uploads file only and in correct section
      # folders has sub folders --> sections created
        # subfolder empty
        # subfolder has files

    # failed upload continues

    # with name of an implict folder in root

    # with the name of a non-existant file

    # with the name of a non-existant folder

    # with the name of a file in a sub folder
      # section already exists
      # section doesn't exist --> is created

    # with name of a file inside an implicit folder

    # file in ignore list

    # folder in ignore list
    end

    context 'CMS has some intersection with local files' do
      # TODO matching file is udated, non-matching is created,
    end

    context 'CMS content list matches local files' do
      context 'CMS and local timestamps match' do
        #TODO
      end

      context 'CMS has some timestamps the same and some newer than local files' do
        #TODO
      end

      context 'CMS has some timestamps the same and some older than local files' do
        #TODO
      end
    end
  end

  context 'Local files are in the CMS' do
    file_filename = 'a_file.jpg'

    before(:each) do
      @cms_api = make_cms BASE_CMS # FIXME warnings in base cms due to implicit folder are distracting in rspec output
      @subject = Threescale::CMS::Cms.new(@cms_api, @local_files)
    end

    describe '#upload' do
      it 'should update a file if it exists in the CMS and local copy is newer' do
        make_file @local_files, file_filename, true
        directory_entries @local_files, '.', [file_filename]
        expect(@cms_api).to receive(:file_update).with("/#{file_filename}", anything, anything, file_filename).and_return(['4', Time.now.to_s])
        @subject.upload file_filename
      end

      it 'should not update a file if it exists in the CMS, but the local copy is not newer' do
        make_file @local_files, file_filename, false
        directory_entries @local_files, '.', [file_filename]
        expect(@cms_api).not_to receive(:file_update)
        @subject.upload file_filename
      end
    end
  end
end
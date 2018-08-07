require_relative '../../cms'
require_relative 'cms_helper'
require_relative 'local_file_helper'

shared_examples_for 'DeleteAll' do |filename|
  before {
    @local_files = make_local_files
  }

  describe '#delete' do
    it 'should delete files in root folder' do
      make_files @local_files, '.', ['a_file.jpg']

      cms = {
          :FILES => FILES,
          :SECTIONS => ROOT_SECTION_ONLY,
          :TEMPLATES => {}
      }
      cms_api = make_cms cms
      @subject = Threescale::CMS::Cms.new(cms_api, @local_files)

      expect(cms_api).to receive(:delete).with(:file, '129')
      @subject.delete filename
    end

    it 'should delete a template in the root folder' do
      make_files @local_files, '.', ['index.html.liquid']

      cms = {
          :FILES => {},
          :SECTIONS => ROOT_SECTION_ONLY,
          :TEMPLATES => INDEX_ONLY
      }
      cms_api = make_cms cms
      @subject = Threescale::CMS::Cms.new(cms_api, @local_files)

      expect(cms_api).to receive(:delete).with(:template, '123')
      @subject.delete filename
    end

    it 'should delete a sub directory of root' do
      make_directory @local_files, '.', 'directory', [], []

      cms = {
          :FILES => {},
          :SECTIONS => ROOT_AND_SUBDIR,
          :TEMPLATES => {}
      }
      cms_api = make_cms cms
      @subject = Threescale::CMS::Cms.new(cms_api, @local_files)

      expect(cms_api).to receive(:delete).with(:section, '2')
      @subject.delete filename
    end

    it 'should not attempt to delete builtin_pages' do
      make_files @local_files, '.', ['show.html.liquid']

      cms = {
          :FILES => {},
          :SECTIONS => ROOT_SECTION_ONLY,
          :TEMPLATES => {
              'show' => {
                  :system_name => 'show',
                  :kind => :template,
                  :type => 'builtin_page',
                  :id => '128',
                  :liquid_enabled => true
              },
          }
      }
      cms_api = make_cms cms
      @subject = Threescale::CMS::Cms.new(cms_api, @local_files)
      expect { @subject.delete filename }.to output(/Skipping deletion/).to_stdout
    end

    it 'should continue deleting if one delete attempt fails' do
      make_files @local_files, '.', ['a_file.jpg', 'index.html.liquid']

      cms = {
          :FILES => FILES,
          :SECTIONS => ROOT_SECTION_ONLY,
          :TEMPLATES => INDEX_ONLY
      }
      cms_api = make_cms cms
      @subject = Threescale::CMS::Cms.new(cms_api, @local_files)

      # force an exception on delete of file
      allow(cms_api).to receive(:delete).with(:file, '129').and_raise('Delete of file failed')

      expect(cms_api).to receive(:delete).with(:template, '123')
      @subject.delete filename
    end
  end
end

describe Threescale::CMS::Cms, fakefs: true do
  context 'no filename specified - deletes all in CMS' do
    it_behaves_like 'DeleteAll', nil
  end

  context "'.' filename specified - deletes based on content found in specified folder" do
    it_behaves_like 'DeleteAll', '.'
  end

  it 'should delete all in a subfolder and the folder' do
    skip
  end

  it 'should delete a specified file' do
    skip
  end
end
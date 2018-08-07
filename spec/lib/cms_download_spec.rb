require_relative '../../cms'
require_relative 'cms_helper'

describe Threescale::CMS::Cms do
  describe '#download' do
    context 'CMS has normal contents' do
      before(:each) do
        @cms_api = make_cms BASE_CMS
        @local_files = double 'local_files'
        @subject = Threescale::CMS::Cms.new(@cms_api, @local_files)

        # Enable downloading of all listed in the CMS
        allow(@local_files).to receive(:needs_update).with(anything, anything).and_return(false) # Not update
        allow(@local_files).to receive(:needs_creation).with(anything).and_return(true)          # Create needed!
        allow(@cms_api).to receive(:file_get).with(anything)
        allow(@cms_api).to receive(:template_get).with(anything)
        allow(@local_files).to receive(:fetch_and_save_file).with(anything, anything, anything)
        allow(@local_files).to receive(:save_template).with(anything, anything, anything)
        allow(@local_files).to receive(:save_section).with(anything, anything)
      end

      it 'should download and create CMS sections if used without a filename' do
        update_time = Time.now
        expect(@local_files).to receive(:save_section).with('.', anything).and_return(update_time)
        expect(@local_files).to receive(:save_section).with('directory', anything).and_return(update_time)

        @subject.download
      end

      it 'should download and create CMS files if used without a filename' do
        file_url = 'https://test.com/a_file.jpg'
        expect(@cms_api).to receive(:file_get).with('129').and_return(file_url)
        expect(@local_files).to receive(:fetch_and_save_file).with('a_file.jpg', file_url, anything)

        @subject.download
      end

      it 'should download and create CMS templates if used without a filename' do
        expect(@cms_api).to receive(:template_get).with('123').and_return('index')
        expect(@cms_api).to receive(:template_get).with('124').and_return('liquid_page')
        expect(@cms_api).to receive(:template_get).with('126').and_return('style.css')
        expect(@cms_api).to receive(:template_get).with('127').and_return('script.js')
        expect(@cms_api).to receive(:template_get).with('128').and_return('show')
        expect(@cms_api).to receive(:template_get).with('3').and_return('main_layout')
        expect(@cms_api).to receive(:template_get).with('4').and_return('partial')

        @subject.download
      end

      it 'should show an error if you attempt to download a named file that is not in the CMS' do
        expect { @subject.download 'non-existant-file.html' }.to raise_exception(/not found/)
      end

      it 'should download the non_liquid_page template, but without .liquid extension' do
        local_path = "#{NON_LIQUID_PAGE}.html"
        allow(@cms_api).to receive(:template_get)
        allow(@local_files).to receive(:needs_update).with(local_path, anything).and_return(true)
        allow(@local_files).to receive(:directory_entries).with(local_path).and_return([])
        expect(@local_files).to receive(:save_template).with(local_path, anything, anything)
        @subject.download local_path
      end

      it 'should download a page with liquid enabled with the .liquid suffix' do
        local_path = "#{LIQUID_PAGE}.html.liquid"
        allow(@cms_api).to receive(:template_get)
        allow(@local_files).to receive(:needs_update).with(local_path, anything).and_return(true)
        allow(@local_files).to receive(:directory_entries).with(local_path).and_return([])
        expect(@local_files).to receive(:save_template).with(local_path, anything, anything)
        @subject.download local_path
      end
    end
  end
end
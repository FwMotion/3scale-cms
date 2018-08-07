require_relative '../../cms'
require_relative 'cms_helper'

describe Threescale::CMS::Cms do
  describe '#info' do
    context 'CMS is empty' do
      before(:each) do
        [:section, :file, :template].each do |kind|
          allow(@cms_api).to receive(:list).with(kind).and_return({})
        end
      end
    end

    # TODO use a shared context
    context 'CMS has contents with implicit folders' do
      before(:each) do
        @cms_api = make_cms IMPLICIT_FOLDER_CMS
        @local_files = double 'local_files'
        @subject = Threescale::CMS::Cms.new(@cms_api, @local_files)
      end

      it 'should show a warning about implicit folders when getting the list' do
        expect(@local_files).to receive(:info).with(nil)
        expect { @subject.info nil }.to output(/includes a folder/).to_stderr_from_any_process
      end

      it 'should show how many implicit folders exist' do
        expect(@local_files).to receive(:info).with(nil)
        expect { @subject.info nil }.to output(/1 implicit folders/).to_stdout
      end

      it 'should list the implicit folders in details' do
        expect(@local_files).to receive(:info).with('details')
        expect { @subject.info 'details' }.to output(/implicit_folder/).to_stdout
      end
    end

    context 'CMS has normal contents' do
      before(:each) do
        @cms_api = make_cms BASE_CMS
        @local_files = double 'local_files'
        @subject = Threescale::CMS::Cms.new(@cms_api, @local_files)
      end

      it 'should output the list of folders in details' do
        expect(@local_files).to receive(:info).with('details')
        expect { @subject.info 'details' }.to output(/.\//).to_stdout
      end

      it 'should output the list of files in details' do
        expect(@local_files).to receive(:info).with('details')
        expect { @subject.info 'details' }.to output(/a_file.jpg/).to_stdout
      end

      it 'should output the index templates in details' do
        expect(@local_files).to receive(:info).with('details')
        expect { @subject.info 'details' }.to output(/index.html.liquid/).to_stdout
      end

      it 'should output the main_layout templates in details' do
        expect(@local_files).to receive(:info).with('details')
        expect { @subject.info 'details' }.to output(/main_layout.html.liquid/).to_stdout
      end

      it 'should output the liquid_page templates in details' do
        expect(@local_files).to receive(:info).with('details')
        expect { @subject.info 'details' }.to output(/liquid_page.html.liquid/).to_stdout
      end

      # TODO combine these next two into one when I can get the matching with tab to work
      # to match ".html\ttemplate"
      it 'should output the non_liquid_page templates in details, but without .liquid extension' do
        expect(@local_files).to receive(:info).with('details')
        expect { @subject.info 'details' }.to output(/non_liquid_page.html/).to_stdout
      end

      it 'should output the non_liquid_page templates in details, but without .liquid extension' do
        expect(@local_files).to receive(:info).with('details')
        expect { @subject.info 'details' }.not_to output(/non_liquid_page.html.liquid/).to_stdout
      end

      it 'should output the files treated as templates in details, but without .liquid extension' do
        expect(@local_files).to receive(:info).with('details')
        expect { @subject.info 'details' }.to output(/style.css/).to_stdout
      end

      it 'should output the files treated as templates in details, but without .liquid extension' do
        expect(@local_files).to receive(:info).with('details')
        expect { @subject.info 'details' }.to output(/script.js/).to_stdout
      end

      it 'should output the built_in templates in details' do
        expect(@local_files).to receive(:info).with('details')
        expect { @subject.info 'details' }.to output(/show.html.liquid/).to_stdout
      end

      it 'should output the partial templates in details' do
        expect(@local_files).to receive(:info).with('details')
        expect { @subject.info 'details' }.to output(/partial.html.liquid/).to_stdout
      end

      it "should map the root path 'root' to '/ '" do
        expect(@local_files).to receive(:info).with('details')
        expect{ @subject.info 'details' }.to output(/'index.html.liquid'/).to_stdout
      end

      it "should map the index path '/' to 'index.html.liquid'" do
        expect(@local_files).to receive(:info).with('details')
        expect{ @subject.info 'details' }.to output(/'index.html.liquid'/).to_stdout
      end

      it 'should list the layouts before the pages' do
        expect(@local_files).to receive(:info).with('details')
        expect{ @subject.info 'details' }.to output(/'l_main_layout.html.liquid'.*'liquid_page.html.liquid'/m).to_stdout
      end
    end
  end
end
require_relative '../../mapper'
require 'fakefs/safe'

describe Threescale::CMS::Mapper, fakefs: true do
  describe '#local_key_from_cms_key' do
    it 'should raise an exception for unknown cms entry kind' do
      cms_entry = {
          :kind => :nonexistent
      }
      expect{ Threescale::CMS::Mapper.local_key_from_cms_key 'key', cms_entry}.to raise_exception(/Unknown/)
    end
  end

  describe '#content_type' do
    it 'should identify a file with html extension as text/html' do
      expect(Threescale::CMS::Mapper.content_type 'file.html').to eq 'text/html'
    end
  end

  describe '#local_info_from_requested_path' do
    let(:local_files) { double }

    it 'should raise an exception if requested local path does not exist' do
      allow(local_files).to receive(:in_list).with(anything).and_return(false)
      expect{ Threescale::CMS::Mapper.local_info_from_requested_path local_files, 'index.html.liquid'}.to raise_exception(/No content found/)
    end

    it "should map '/' onto index.html.liquid as a page" do
      allow(local_files).to receive(:in_list).with(anything).and_return(false)
      allow(local_files).to receive(:in_list).with('index.html.liquid').and_return(true)
      path, kind = Threescale::CMS::Mapper.local_info_from_requested_path local_files, '/'
      expect(path).to eq 'index.html.liquid'
      expect(kind).to eq :template
    end

    it "should map 'script.js' onto 'script.js' as a file" do
      allow(local_files).to receive(:in_list).with(anything).and_return(false)
      allow(local_files).to receive(:in_list).with('script.js').and_return(true)
      path, kind = Threescale::CMS::Mapper.local_info_from_requested_path local_files, 'script.js'
      expect(path).to eq 'script.js'
      expect(kind).to eq :file
    end
  end

  describe '#local_key_from_cms_key' do
    it 'raises exception on invalid cms kind' do
      cms_entry = { :kind => :fake }
      expect{ Threescale::CMS::Mapper.local_key_from_cms_key '/file.js', cms_entry }.to raise_exception(/Unknown/)
    end

    it 'raises exception on invalid cms template type' do
      cms_entry = {
          :kind => :template,
          :type => 'rubbish'
      }
      expect{ Threescale::CMS::Mapper.local_key_from_cms_key '/file.js', cms_entry }.to raise_exception(/Unknown/)
    end

    context 'file' do
      it 'strips leading / off file in root section' do
        cms_entry = { :kind => :file }
        expect(Threescale::CMS::Mapper.local_key_from_cms_key '/file.js', cms_entry).to eq 'file.js'
      end

      it 'strips leading / off file in sub section' do
        cms_entry = { :kind => :file }
        expect(Threescale::CMS::Mapper.local_key_from_cms_key '/subdir/file.js', cms_entry).to eq 'subdir/file.js'
      end
    end

    context 'section' do
      it "maps '/' to local '.'" do
        cms_entry = { :kind => :section }
        expect(Threescale::CMS::Mapper.local_key_from_cms_key '/', cms_entry).to eq '.'
      end

      it "maps 'root' to local '.'" do
        cms_entry = { :kind => :section }
        expect(Threescale::CMS::Mapper.local_key_from_cms_key 'root', cms_entry).to eq '.'
      end

      it 'maps other section to relative subdir of same name' do
        cms_entry = { :kind => :section }
        expect(Threescale::CMS::Mapper.local_key_from_cms_key 'subdir', cms_entry).to eq 'subdir'
      end
    end

    context 'template' do
      it 'adds layout prefix to layout template based on its system_name' do
        cms_entry = {
            :kind => :template,
            :type => 'layout'
        }
        expect(Threescale::CMS::Mapper.local_key_from_cms_key 'layout', cms_entry).to eq 'l_layout.html'
      end
    end

    context 'page' do
      it 'adds page suffix to page template in root based on its path and makes relative path' do
        cms_entry = {
            :kind => :template,
            :type => 'page'
        }
        expect(Threescale::CMS::Mapper.local_key_from_cms_key '/page', cms_entry).to eq 'page.html'
      end

      it 'adds page suffix to page template in subdir based on its path and makes relative path' do
        cms_entry = {
            :kind => :template,
            :type => 'page'
        }
        expect(Threescale::CMS::Mapper.local_key_from_cms_key '/subdir/page', cms_entry).to eq 'subdir/page.html'
      end

      it 'adds page suffix to builting page template in root based on its path and makes relative path' do
        cms_entry = {
            :kind => :template,
            :type => 'builtin_page'
        }
        expect(Threescale::CMS::Mapper.local_key_from_cms_key '/builtingpage', cms_entry).to eq 'builtingpage.html'
      end

      it "maps index page at path '/' onto local index filename" do
        cms_entry = {
            :kind => :template,
            :type => 'page'
        }
        expect(Threescale::CMS::Mapper.local_key_from_cms_key '/', cms_entry).to eq 'index.html'
      end
    end

    context 'partial' do
      it 'adds partial prefix to partial template based on its system_name' do
        cms_entry = {
            :kind => :template,
            :type => 'partial'
        }
        expect(Threescale::CMS::Mapper.local_key_from_cms_key 'partial', cms_entry).to eq '_partial.html'
      end

      it 'adds partial prefix to builtin partial template based on its system_name' do
        cms_entry = {
            :kind => :template,
            :type => 'builtin_partial'
        }
        expect(Threescale::CMS::Mapper.local_key_from_cms_key 'partial', cms_entry).to eq '_partial.html'
      end
    end
  end

  describe '#kind_and_type_from_path' do
    it 'detects kind and type of a page correctly' do
      kind, type = Threescale::CMS::Mapper.kind_and_type_from_path 'index.html.liquid'
      expect(kind).to eq :template
      expect(type).to eq 'page'
    end

    it 'detects kind and type of a partial correctly' do
      kind, type = Threescale::CMS::Mapper.kind_and_type_from_path '_partial.html'
      expect(kind).to eq :template
      expect(type).to eq 'partial'
    end

    it 'detects kind and type of a layout correctly' do
      kind, type = Threescale::CMS::Mapper.kind_and_type_from_path 'l_layout.html'
      expect(kind).to eq :template
      expect(type).to eq 'layout'
    end

    it 'detects kind of a section correctly' do
      Dir.mkdir 'subdir'
      kind, _ = Threescale::CMS::Mapper.kind_and_type_from_path 'subdir'
      expect(kind).to eq :section
    end

    it 'detects kind of a file correctly' do
      FileUtils.touch 'file.png'
      kind, _ = Threescale::CMS::Mapper.kind_and_type_from_path 'file.png'
      expect(kind).to eq :file
    end
  end

  describe '#cms_section_key_from_local_section_key' do
    it "maps '.' to 'root'" do
      expect(Threescale::CMS::Mapper.cms_section_key_from_local_section_key '.').to eq Threescale::CMS::Mapper::ROOT_OTHER_PARTIAL_PATH
    end

    it 'maps other dir to same section' do
      expect(Threescale::CMS::Mapper.cms_section_key_from_local_section_key 'subdir').to eq 'subdir'
    end
  end

  describe '#local_section_key_from_path' do
    it 'returns root section for a file not in a sub directory' do
      expect(Threescale::CMS::Mapper.local_section_key_from_path 'index.html').to eq Threescale::CMS::Mapper::ROOT_LOCAL_KEY
    end

    it 'returns sub section for a file in a sub directory' do
      expect(Threescale::CMS::Mapper.local_section_key_from_path 'subdir/index.html').to eq 'subdir'
    end
  end

  describe '#cmsinfo_from_path' do
    it 'detects is liquid page' do
      _, _, _, liquid = Threescale::CMS::Mapper.cmsinfo_from_path 'index.html.liquid'
      expect(liquid).to eq 1
    end

    it 'detects is not liquid page' do
      _, _, _, liquid = Threescale::CMS::Mapper.cmsinfo_from_path 'page.html'
      expect(liquid).to eq 0
    end

    it "strips off '.liquid' and '.html' suffixes and makes absolute cms path" do
      _, _, path, _ = Threescale::CMS::Mapper.cmsinfo_from_path 'page.html.liquid'
      expect(path).to eq '/page'
    end

    it "strips off '.html' suffixes and makes absolute cms path" do
      _, _, path, _ = Threescale::CMS::Mapper.cmsinfo_from_path 'page.html'
      expect(path).to eq '/page'
    end

    it "strips off 'l_' layout prefix and makes absolute cms path" do
      _, _, path, _ = Threescale::CMS::Mapper.cmsinfo_from_path 'l_layout.html'
      expect(path).to eq '/layout'
    end

    it "strips off '_' partial prefix and makes absolute cms path" do
      _, _, path, _ = Threescale::CMS::Mapper.cmsinfo_from_path '_partial.html'
      expect(path).to eq '/partial'
    end

    it "maps 'index.html.liquid' to '/'" do
      _, _, path, _ = Threescale::CMS::Mapper.cmsinfo_from_path 'index.html.liquid'
      expect(path).to eq '/'
    end

    it 'sets title to be basename of longer path after removing suffixes' do
      title, _, _, _ = Threescale::CMS::Mapper.cmsinfo_from_path 'dir/index.html.liquid'
      expect(title).to eq 'index'
    end

    it "sets system_name to be path without leading '/'" do
      _, system_name, _, _ = Threescale::CMS::Mapper.cmsinfo_from_path 'dir/page.html.liquid'
      expect(system_name).to eq 'dir/page'
    end
  end
end
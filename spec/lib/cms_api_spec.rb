require_relative '../../cms_api'
require_relative 'local_file_helper'

describe Threescale::CMS::Api do
  PROVIDER_KEY = 'provider_key'
  TEST_URL = 'https://test-admin.3scale.net'
  CMS_PATH = '/admin/api/cms'
  CMS_URL = TEST_URL + CMS_PATH
  xml_header = '<?xml version="1.0"?>'

  context 'force errors returned by API' do
    before(:each) do
      @subject = Threescale::CMS::Api.new PROVIDER_KEY, TEST_URL
    end

    it 'raises an exception if API response shows error occurred' do
      parameters = '?provider_key=provider_key'
      response_body = xml_header + '<error>An error occurred</error>'
      stub_request(:get, CMS_URL + '/files/3.xml' + parameters).to_return(:status => 200, :body => response_body, :headers => {})
      expect { @subject.file_get '3' }.to raise_exception(/An error/)
    end

    it 'raises an exception if request to API raises an exception' do
      parameters = '?provider_key=provider_key'
      stub_request(:get, CMS_URL + '/files/3.xml' + parameters).to_raise(RestClient::UnprocessableEntity)
      expect { @subject.file_get '3' }.to raise_exception(/Error performing/)
    end
  end

  context 'invalid parameters to new' do
    describe '#initialize' do
      it 'should raise an exception if initialized without a provider_key' do
        expect { Threescale::CMS::Api.new '', 'invalid url' }.to raise_exception(/invalid/)
      end

      it 'should raise an exception if initialized with an invalid CMS url' do
        expect { Threescale::CMS::Api.new 'provider_key', 'invalid url' }.to raise_exception(/invalid/)
      end
    end
  end

  context 'invalid parameters to methods' do
    valid_path = '/file'
    invalid_path = 'file'
    valid_id = '1'
    invalid_id = 'not_an_id'
    valid_filename = './file.js'
    invalid_filename = '/non-existant'
    valid_tag_list = ''
    valid_type = 'page'
    invalid_type = 'invalid_type'
    valid_section_name = 'folder'
    invalid_section_name = '/folder' # TBD
    valid_system_name = 'my-system-name'
    invalid_system_name = 'invalid.system.name'
    valid_title = 'title'
    invalid_title = '' # TBD
    valid_layout_name = 'layout'
    invalid_layout_name = 'lay out name'

    before(:each) do
      @subject = Threescale::CMS::Api.new('provider_key', 'https://test-admin.3scale.net')
    end

    describe '#list' do
      it 'should raise an exception if kind parameter is invalid' do
        expect { @subject.list :fake }.to raise_exception(/Invalid/)
      end
    end

    describe '#delete' do
      it 'should raise an exception if kind parameter is invalid' do
        expect { @subject.delete :fake, 1 }.to raise_exception(/Invalid/)
      end

      it 'should raise an exception if id parameter is invalid' do
        expect { @subject.delete :file, 'not_an_id' }.to raise_error(/invalid value for Integer/)
      end
    end

    describe '#file_update' do
      it 'should raise an exception if path parameter is invalid' do
        expect { @subject.file_update invalid_path, valid_id, valid_id, valid_filename }.to raise_exception(/Invalid/)
      end

      it 'should raise an exception if id parameter is invalid' do
        expect { @subject.file_update valid_path, invalid_id, valid_id, valid_filename }.to raise_error(/invalid value for Integer/)
      end

      it 'should raise an exception if section_id parameter is invalid' do
        expect { @subject.file_update valid_path, valid_id, invalid_id, valid_filename }.to raise_error(/invalid value for Integer/)
      end

      it 'should raise an exception if filename parameter is invalid' do
        expect { @subject.file_update valid_path, valid_id, valid_id, invalid_filename }.to raise_exception(/Invalid/)
      end
    end

    describe '#file_create' do
      it 'should raise an exception if path parameter is invalid' do
        expect { @subject.file_create invalid_path, valid_id, valid_filename, valid_tag_list }.to raise_exception(/Invalid/)
      end

      it 'should raise an exception if section_id parameter is invalid' do
        expect { @subject.file_create valid_path, invalid_id, valid_filename, valid_tag_list }.to raise_error(/invalid value for Integer/)
      end

      it 'should raise an exception if filename parameter is invalid' do
        expect { @subject.file_create valid_path, valid_id, invalid_filename, valid_tag_list }.to raise_exception(/Invalid/)
      end
    end

    describe '#file_get' do
      it 'should raise an exception if id parameter is invalid' do
        expect { @subject.file_get invalid_id }.to raise_error(/invalid value for Integer/)
      end
    end

    describe '#template_get' do
      it 'should raise an exception if id parameter is invalid' do
        expect { @subject.template_get invalid_id }.to raise_error(/invalid value for Integer/)
      end
    end

    describe '#template_update' do
      it 'should raise an exception if id parameter is invalid' do
        expect { @subject.template_update invalid_id, valid_type, valid_filename }.to raise_error(/invalid value for Integer/)
      end

      it 'should raise an exception if type parameter is invalid' do
        expect { @subject.template_update valid_id, invalid_type, valid_filename }.to raise_exception(/Invalid/)
      end

      it 'should raise an exception if filename parameter is invalid' do
        expect { @subject.template_update valid_id, valid_type, invalid_filename }.to raise_exception(/Invalid/)
      end
    end

    describe '#template_create' do
      it 'should raise an exception if path parameter is invalid' do
        expect { @subject.template_create invalid_path, valid_section_name, valid_id, valid_system_name,
                                          valid_title, valid_layout_name, valid_type, valid_filename, false }.to raise_exception(/Invalid/)
      end

      it 'should raise an exception if section_name parameter is invalid' do
        expect { @subject.template_create valid_path, invalid_section_name, valid_id, valid_system_name,
                                          valid_title, valid_layout_name, valid_type, valid_filename, false }.to raise_exception(/Invalid/)
      end

      it 'should raise an exception if section_id parameter is invalid' do
        expect { @subject.template_create valid_path, valid_section_name, invalid_id, valid_system_name,
                                          valid_title, valid_layout_name, valid_type, valid_filename, false }.to raise_error(/invalid value for Integer/)
      end

      it 'should raise an exception if the files does not exist' do
        expect { @subject.template_create valid_path, valid_section_name, valid_id, valid_system_name,
                                          valid_title, valid_layout_name, valid_type, valid_filename, false }.to raise_exception(/file-not-found/)
      end

      it 'should raise an exception if system_name parameter is invalid', fakefs: true do
        local_files = make_local_files
        make_files local_files, '.', ['partial.html.liquid']

        expect { @subject.template_create valid_path, valid_section_name, valid_id, invalid_system_name,
                                          valid_title, valid_layout_name, 'partial', 'partial.html.liquid', false }.to raise_exception(/Invalid/)
      end

      it 'should raise an exception if title parameter is invalid' do
        pending('To implement title syntax checking')
        expect { @subject.template_create valid_path, valid_section_name, valid_id, valid_system_name,
                                          invalid_title, valid_layout_name, valid_type, valid_filename, false }.to raise_exception(/Invalid title/)
      end

      it 'should raise an exception if layout_name parameter is invalid' do
        pending('To implement layout_name syntax checking')
        expect { @subject.template_create valid_path, valid_section_name, valid_id, valid_system_name,
                                          valid_title, invalid_layout_name, valid_type, valid_filename, false }.to raise_exception(/Invalid layout_name/)
      end

      it 'should raise an exception if type parameter is invalid' do
        expect { @subject.template_create valid_path, valid_section_name, valid_id, valid_system_name,
                                          valid_title, valid_layout_name, invalid_type, valid_filename, false }.to raise_exception(/Invalid/)
      end

      it 'should raise an exception if filename parameter is invalid' do
        expect { @subject.template_create valid_path, valid_section_name, valid_id, valid_system_name,
                                          valid_title, valid_layout_name, valid_type, invalid_filename, false }.to raise_exception(/Invalid/)
      end
    end

    describe '#section_get' do
      it 'should raise an exception if id parameter is invalid' do
        expect { @subject.section_get invalid_id }.to raise_error(/invalid value for Integer/)
      end
    end

    describe '#section_update' do
      it 'should raise an exception if id parameter is invalid' do
        expect { @subject.section_update invalid_id, valid_title, valid_id, valid_path }.to raise_error(/invalid value for Integer/)
      end

      it 'should raise an exception if title parameter is invalid' do
        pending('To implement title syntax checking')
        expect { @subject.section_update valid_id, invalid_title, valid_id, valid_path }.to raise_exception(/Invalid/)
      end

      it 'should raise an exception if parent_id parameter is invalid' do
        expect { @subject.section_update valid_id, valid_title, invalid_id, valid_path }.to raise_error(/invalid value for Integer/)
      end
    end

    describe '#section_create' do
      it 'should raise an exception if title parameter is invalid' do
        pending('To implement title syntax checking')
        expect { @subject.section_create valid_path, invalid_title, valid_id }.to raise_exception(/Invalid/)
      end

      it 'should raise an exception if parent_id parameter is invalid' do
        expect { @subject.section_create valid_path, valid_title, invalid_id }.to raise_error(/invalid value for Integer/)
      end
    end
  end

  context 'Valid parameters, check behaviour' do
    before(:each) do
      @subject = Threescale::CMS::Api.new PROVIDER_KEY, TEST_URL
    end

    describe '#initialize' do
      it 'should form the correct CMS API URI' do
        @subject.base_url == CMS_URL
      end
    end

    describe '#get_content_list' do
      sections_path = 'sections.xml'
      files_path = 'files.xml'
      templates_path = 'templates.xml'
      page1_parameters = 'page=1&per_page=100&provider_key=provider_key'
      page2_parameters = 'page=2&per_page=100&provider_key=provider_key'
      empty_section = '<section></section>'
      invalid_sections_list = "<sections per_page='100' total_entries='1' total_pages='1' current_page='1'>
                                  <section>
                                    <id>3891</id>
                                    <created_at>2012-08-14T08:25:24Z</created_at>
                                    <updated_at>2016-03-08T22:04:48Z</updated_at>
                                    <public>true</public>
                                    <title>Root</title>
                                    <parent_id/>
                                    <system_name>root</system_name>
                                  </section>
                                </sections>"
      valid_layout_list = "<templates per_page='100' total_entries='1' total_pages='1' current_page='1'>
                                 <layout>
                                  <id>14931</id>
                                  <created_at>2010-11-17T18:12:53Z</created_at>
                                  <updated_at>2013-11-29T10:41:35Z</updated_at>
                                  <system_name>access_denied_error</system_name>
                                  <content_type>text/html</content_type>
                                  <handler/>
                                  <liquid_enabled>false</liquid_enabled>
                                  <title>valid layout list</title>
                                </layout>
                              </templates>"
      full_template_list = "<templates per_page='100' total_entries='1' total_pages='1' current_page='1'>
                                 <layout>
                                  <id>1</id>
                                  <created_at>2010-11-17T18:12:53Z</created_at>
                                  <updated_at>2013-11-29T10:41:35Z</updated_at>
                                  <system_name>access_denied_error</system_name>
                                  <content_type>text/html</content_type>
                                  <handler/>
                                  <liquid_enabled>false</liquid_enabled>
                                  <title>valid layout list</title>
                                </layout>
                                 <page>
                                  <id>2</id>
                                  <path>/page</path>
                                  <created_at>2010-11-17T18:12:53Z</created_at>
                                  <updated_at>2013-11-29T10:41:35Z</updated_at>
                                  <content_type>text/html</content_type>
                                  <handler/>
                                  <liquid_enabled>false</liquid_enabled>
                                  <title>page</title>
                                </page>
                                 <builtin_page>
                                  <id>3</id>
                                  <path>/show</path>
                                  <system_name>show</system_name>
                                  <created_at>2010-11-17T18:12:53Z</created_at>
                                  <updated_at>2013-11-29T10:41:35Z</updated_at>
                                  <content_type>text/html</content_type>
                                  <handler/>
                                  <liquid_enabled>false</liquid_enabled>
                                  <title>show</title>
                                </builtin_page>
                                 <partial>
                                  <id>4</id>
                                  <path>/partial</path>
                                  <system_name>partial</system_name>
                                  <created_at>2010-11-17T18:12:53Z</created_at>
                                  <updated_at>2013-11-29T10:41:35Z</updated_at>
                                  <content_type>text/html</content_type>
                                  <liquid_enabled>false</liquid_enabled>
                                  <title>partial</title>
                                </partial>
                                 <builtin_partial>
                                  <id>5</id>
                                  <path>/builtin</path>
                                  <system_name>builtin_partial</system_name>
                                  <created_at>2010-11-17T18:12:53Z</created_at>
                                  <updated_at>2013-11-29T10:41:35Z</updated_at>
                                  <content_type>text/html</content_type>
                                  <liquid_enabled>false</liquid_enabled>
                                  <title>builtin partial</title>
                                </builtin_partial>
                              </templates>"
      invalid_page_list = "<templates per_page='100' total_entries='1' total_pages='1' current_page='1'>
                                 <page>
                                  <id>14931</id>
                                  <created_at>2010-11-17T18:12:53Z</created_at>
                                  <updated_at>2013-11-29T10:41:35Z</updated_at>
                                  <content_type>text/html</content_type>
                                  <handler/>
                                  <liquid_enabled>false</liquid_enabled>
                                  <title>invalid page list</title>
                                </page>
                              </templates>"
      layout_unknown_type = "<templates per_page='100' total_entries='1' total_pages='1' current_page='1'>
                                 <unknown>
                                  <id>14931</id>
                                  <created_at>2010-11-17T18:12:53Z</created_at>
                                  <updated_at>2013-11-29T10:41:35Z</updated_at>
                                  <content_type>text/html</content_type>
                                  <handler/>
                                  <system_name>/themes</system_name>
                                  <liquid_enabled>false</liquid_enabled>
                                  <title>Access denied error</title>
                                </unknown>
                              </templates>"
      layout_without_system_name = "<templates per_page='100' total_entries='1' total_pages='1' current_page='1'>
                                 <page>
                                  <id>14931</id>
                                  <created_at>2010-11-17T18:12:53Z</created_at>
                                  <updated_at>2013-11-29T10:41:35Z</updated_at>
                                  <content_type>text/html</content_type>
                                  <handler/>
                                  <liquid_enabled>false</liquid_enabled>
                                  <title>Access denied error</title>
                                </page>
                              </templates>"
      valid_sections_list = "<sections per_page='100' total_entries='1' total_pages='1' current_page='1'>
                                 <section>
                                  <id>3911</id>
                                  <created_at>2012-08-14T08:25:24Z</created_at>
                                  <updated_at>2012-08-14T08:25:24Z</updated_at>
                                  <partial_path>/themes</partial_path>
                                  <public>true</public>
                                  <title>Themes</title>
                                  <parent_id>3891</parent_id>
                                  <system_name>/themes</system_name>
                                  </section>
                                </sections>"
      valid_files_list = "<files per_page='100' total_entries='1' total_pages='1' current_page='1'>
                              <file>
                                  <id>5731</id>
                                  <created_at>2011-01-03T14:49:24Z</created_at>
                                  <updated_at>2011-01-03T14:49:25Z</updated_at>
                                  <section_id>3951</section_id>
                                  <path>/application-gallery/images/app-gallery-api-portal.png</path>
                                  <url>https://s3.amazonaws.com/enterprise-multitenant.3scale.net.3scale.net/3scale-docs/2011/01/03/app-gallery-api-portal-4d62365b.png?AWSAccessKeyId=AKIAIRYLTWBQ37ZNGBZA&amp;Expires=1457534793&amp;Signature=mk0%2BCv%2BHsyrI2gpTITeVww1otpE%3D</url>
                                  <tag_list>application, gallery</tag_list>
                                  <title>app-gallery-api-portal.png</title>
                                </file>
                            </files>"
      invalid_files_list = "<files per_page='100' total_entries='1' total_pages='1' current_page='1'>
                              <file>
                                  <id>5731</id>
                                  <created_at>2011-01-03T14:49:24Z</created_at>
                                  <updated_at>2011-01-03T14:49:25Z</updated_at>
                                  <path>/application-gallery/images/app-gallery-api-portal.png</path>
                                  <url>https://s3.amazonaws.com/enterprise-multitenant.3scale.net.3scale.net/3scale-docs/2011/01/03/app-gallery-api-portal-4d62365b.png?AWSAccessKeyId=AKIAIRYLTWBQ37ZNGBZA&amp;Expires=1457534793&amp;Signature=mk0%2BCv%2BHsyrI2gpTITeVww1otpE%3D</url>
                                  <tag_list>application, gallery</tag_list>
                                  <title>app-gallery-api-portal.png</title>
                                </file>
                            </files>"

      it 'should raise an exception if a CMS list entry doesnt have obligatory common fields' do
        invalid_section_list = "<sections per_page='100' total_entries='1' total_pages='1' current_page='1'>#{empty_section}</sections>"
        body = xml_header + invalid_section_list
        stub_request(:get, CMS_URL + '/' + sections_path + '?' + page1_parameters).to_return(:status => 200, :body => body, :headers => {})
        expect { @subject.list(:section) }.to raise_exception(/missing/)
      end

      it 'should parse an empty list' do
        empty_section_list = "<sections per_page='100' total_entries='1' total_pages='1' current_page='1'></sections>"
        body = xml_header + empty_section_list
        stub_request(:get, CMS_URL + '/' + sections_path + '?' + page1_parameters).to_return(:status => 200, :body => body, :headers => {})
        expect(@subject.list(:section)).to be_empty
      end

      it 'should parse a template list with all known types in it' do
        body = xml_header + full_template_list
        stub_request(:get, CMS_URL + '/' + templates_path + '?' + page1_parameters).to_return(:status => 200, :body => body, :headers => {})
        @subject.list(:template)
      end

      it 'should parse a valid list of sections' do
        body = xml_header + valid_sections_list
        stub_request(:get, CMS_URL + '/' + sections_path + '?' + page1_parameters).to_return(:status => 200, :body => body, :headers => {})
        expect(@subject.list(:section).size).to eq 1
      end

      it 'should raise an exception if section is missing the partial_path parameter' do
        body = xml_header + invalid_sections_list
        stub_request(:get, CMS_URL + '/' + sections_path + '?' + page1_parameters).to_return(:status => 200, :body => body, :headers => {})
        expect { @subject.list(:section) }.to raise_exception(/missing/)
      end

      it 'should parse a valid list of files' do
        body = xml_header + valid_files_list
        stub_request(:get, CMS_URL + '/' + files_path + '?' + page1_parameters).to_return(:status => 200, :body => body, :headers => {})
        expect(@subject.list(:file).size).to eq 1
      end

      it 'should raise an exception if file is missing the section_id parameter' do
        body = xml_header + invalid_files_list
        stub_request(:get, CMS_URL + '/' + files_path + '?' + page1_parameters).to_return(:status => 200, :body => body, :headers => {})
        expect { @subject.list(:file) }.to raise_exception(/missing/)
      end

      it 'should parse a valid list of layout templates' do
        body = xml_header + valid_layout_list
        stub_request(:get, CMS_URL + '/' + templates_path + '?' + page1_parameters).to_return(:status => 200, :body => body, :headers => {})
        expect(@subject.list(:template).size).to eq 1
      end

      it 'should raise an exception if a layout template is missing the system_name parameter' do
        body = xml_header + layout_without_system_name
        stub_request(:get, CMS_URL + '/' + templates_path + '?' + page1_parameters).to_return(:status => 200, :body => body, :headers => {})
        expect { @subject.list(:template) }.to raise_exception(/missing/)
      end

      it 'should raise an exception if a layout template is of an unknown type' do
        body = xml_header + layout_unknown_type
        stub_request(:get, CMS_URL + '/' + templates_path + '?' + page1_parameters).to_return(:status => 200, :body => body, :headers => {})
        expect { @subject.list(:template) }.to raise_exception(/Unknown type/)
      end

      it 'should raise an exception if a page template is missing the path parameter' do
        body = xml_header + invalid_page_list
        stub_request(:get, CMS_URL + '/' + templates_path + '?' + page1_parameters).to_return(:status => 200, :body => body, :headers => {})
        expect { @subject.list(:template) }.to raise_exception(/missing property/)
      end

      it 'should raise an exception if a page template is missing the liquid_enabled parameter' do
        body = xml_header + invalid_page_list
        stub_request(:get, CMS_URL + '/' + templates_path + '?' + page1_parameters).to_return(:status => 200, :body => body, :headers => {})
        expect { @subject.list(:template) }.to raise_exception(/missing property/)
      end

      it 'should parse out a false liquid_enabled parameter' do
        no_liquid_page_list = "<templates per_page='100' total_entries='1' total_pages='1' current_page='1'>
                                 <page>
                                  <id>14931</id>
                                  <created_at>2010-11-17T18:12:53Z</created_at>
                                  <updated_at>2013-11-29T10:41:35Z</updated_at>
                                  <content_type>text/html</content_type>
                                  <handler/>
                                  <path>page_path</path>
                                  <liquid_enabled>false</liquid_enabled>
                                  <title>Access denied error</title>
                                </page>
                              </templates>"
        body = xml_header + no_liquid_page_list
        stub_request(:get, CMS_URL + '/' + templates_path + '?' + page1_parameters).to_return(:status => 200, :body => body, :headers => {})
        cms_entry = @subject.list(:template)['page_path']
        expect(cms_entry[:liquid_enabled]).to be false
      end

      it 'should parse out a true liquid_enabled parameter' do
        invalid_page_list = "<templates per_page='100' total_entries='1' total_pages='1' current_page='1'>
                                 <page>
                                  <id>14931</id>
                                  <created_at>2010-11-17T18:12:53Z</created_at>
                                  <updated_at>2013-11-29T10:41:35Z</updated_at>
                                  <content_type>text/html</content_type>
                                  <handler/>
                                  <path>page_path</path>
                                  <liquid_enabled>true</liquid_enabled>
                                  <title>Access denied error</title>
                                </page>
                              </templates>"
        body = xml_header + invalid_page_list
        stub_request(:get, CMS_URL + '/' + templates_path + '?' + page1_parameters).to_return(:status => 200, :body => body, :headers => {})
        cms_entry = @subject.list(:template)['page_path']
        expect(cms_entry[:liquid_enabled]).to be true
      end

      it 'should fetch all pages in the response' do
        first_page = "<templates per_page='100' total_entries='21' total_pages='2' current_page='1'>
                         <layout>
                          <id>14931</id>
                          <created_at>2010-11-17T18:12:53Z</created_at>
                          <updated_at>2013-11-29T10:41:35Z</updated_at>
                          <system_name>access_denied_error</system_name>
                          <content_type>text/html</content_type>
                          <handler/>
                          <liquid_enabled>false</liquid_enabled>
                          <title>Access denied error</title>
                        </layout>
                      </templates>"
        second_page = "<templates per_page='100' total_entries='21' total_pages='2' current_page='2'>
                         <layout>
                          <id>14932</id>
                          <created_at>2010-11-17T18:12:53Z</created_at>
                          <updated_at>2013-11-29T10:41:35Z</updated_at>
                          <system_name>other_access_denied_error</system_name>
                          <content_type>text/html</content_type>
                          <handler/>
                          <liquid_enabled>false</liquid_enabled>
                          <title>Other Access denied error</title>
                        </layout>
                      </templates>"

        body1 = xml_header + second_page
        stub_request(:get, CMS_URL + '/' + templates_path + '?' + page1_parameters).to_return(:status => 200, :body => body1, :headers => {})
        body2 = xml_header + first_page
        stub_request(:get, CMS_URL + '/' + templates_path + '?' + page2_parameters).to_return(:status => 200, :body => body2, :headers => {})
        expect(@subject.list(:template).size).to eq 2
      end

      it 'should throw an exception if unexpected error code is returned' do
        stub_request(:get, CMS_URL + '/' + sections_path + '?' + page1_parameters).to_return(:status => 201, :body => '', :headers => {})
        expect { @subject.list(:section) }.to raise_exception(/unexpected response code/)
      end
    end

    describe '#delete' do
      parameters = '&provider_key=provider_key'

      it 'should delete a file - not raising an exception on unexpected response' do
        stub_request(:delete, CMS_URL + '/files/2.xml?id=2' + parameters).to_return(:status => 200, :body => '', :headers => {})
        @subject.delete :file, '2'
      end
    end

    describe '#file_update', fakefs: true do
      it 'should update a file - and return id and updated at' do
        local_files = make_local_files
        make_files local_files, '.', ['file.js']
        updated_at = Time.now
        response_body = xml_header + "<file><id>2</id><updated_at>#{updated_at}</updated_at></file>"
        stub_request(:put, CMS_URL + '/files/2.xml').to_return(:status => 200, :body => response_body, :headers => {})
        expect(@subject.file_update '/file.js', '2', '1', 'file.js').to match_array(['2', anything])
      end
    end

    describe '#file_create', fakefs: true do
      it 'should create a file - and return id and updated at' do
        local_files = make_local_files
        make_files local_files, '.', ['new-file.js']
        updated_at = Time.now
        response_body = xml_header + "<file><id>3</id><updated_at>#{updated_at}</updated_at></file>"
        stub_request(:post, CMS_URL + '/files.xml').to_return(:status => 201, :body => response_body, :headers => {})
        id, _ = @subject.file_create '/new-file.js', '1', 'new-file.js', {}
        expect(id).to eq '3'
      end
    end

    describe '#file_get', fakefs: true do
      it 'should fetch the correct file by id' do
        parameters = '?provider_key=provider_key'
        url = 'https://s3.amazon.com/some/url'
        updated_at = Time.now
        response_body = xml_header + "<file><id>3</id><updated_at>#{updated_at}</updated_at><url>#{url}</url></file>"
        stub_request(:get, CMS_URL + '/files/3.xml' + parameters).to_return(:status => 200, :body => response_body, :headers => {})
        id, _ = @subject.file_get '3'
        expect(id).to eq URI(url)
      end
    end

    describe '#template_get' do
      it 'should fetch the draft template content if only draft content is present' do
        parameters = '?provider_key=provider_key'
        template_content = 'template content'
        response_body = "<draft>#{template_content}</draft>"
        stub_request(:get, CMS_URL + '/templates/3.xml' + parameters).to_return(:status => 200, :body => response_body, :headers => {})
        id, _ = @subject.template_get '3'
        expect(id).to eq template_content
      end

      it 'should fetch the published template content if only published content is present' do
        parameters = '?provider_key=provider_key'
        template_content = 'template content'
        response_body = "<published>#{template_content}</published>"
        stub_request(:get, CMS_URL + '/templates/3.xml' + parameters).to_return(:status => 200, :body => response_body, :headers => {})
        id, _ = @subject.template_get '3'
        expect(id).to eq template_content
      end

      it 'should fetch the published template content if published AND draft content is present' do
        parameters = '?provider_key=provider_key'
        published_template_content = 'published template content'
        draft_template_content = 'draft template content'
        response_body = "<published>#{published_template_content}</published><draft>#{draft_template_content}</draft>"
        stub_request(:get, CMS_URL + '/templates/3.xml' + parameters).to_return(:status => 200, :body => response_body, :headers => {})
        id, _ = @subject.template_get '3'
        expect(id).to eq published_template_content
      end
    end

    describe '#template_create', fakefs: true do
      it 'should create a page template - and return id and updated at' do
        local_files = make_local_files
        make_files local_files, '.', ['page.html.liquid']
        updated_at = Time.now
        response_body = xml_header + "<page><id>3</id><updated_at>#{updated_at}</updated_at></page>"
        stub_request(:post, CMS_URL + '/templates.xml').to_return(:status => 201, :body => response_body, :headers => {})
        id, _ = @subject.template_create '/page', 'root', '1', 'page', 'Page', 'l_layout', 'page', 'page.html.liquid', 1
        expect(id).to eq '3'
      end

      it 'should create a partial template - and return id and updated at' do
        local_files = make_local_files
        make_files local_files, '.', ['_partial.html.liquid']
        updated_at = Time.now
        response_body = xml_header + "<partial><id>3</id><updated_at>#{updated_at}</updated_at></partial>"
        stub_request(:post, CMS_URL + '/templates.xml').to_return(:status => 201, :body => response_body, :headers => {})
        id, _ = @subject.template_create '/partial', 'root', '1', 'partial', 'partial', 'l_layout', 'partial', '_partial.html.liquid', 1
        expect(id).to eq '3'
      end
    end

    describe '#template_update', fakefs: true do
      it 'should update a page template - and return id and updated at' do
        local_files = make_local_files
        make_files local_files, '.', ['page.html.liquid']
        updated_at = Time.now
        response_body = xml_header + "<page><id>2</id><updated_at>#{updated_at}</updated_at></page>"
        stub_request(:put, CMS_URL + '/templates/2.xml').to_return(:status => 200, :body => response_body, :headers => {})
        expect(@subject.template_update '2', 'page', 'page.html.liquid').to match_array(['2', anything])
      end

      it 'should update a partial template - and return id and updated at' do
        local_files = make_local_files
        make_files local_files, '.', ['_partial.html.liquid']
        updated_at = Time.now
        response_body = xml_header + "<partial><id>2</id><updated_at>#{updated_at}</updated_at><system_name>partial</system_name></partial>"
        stub_request(:put, CMS_URL + '/templates/2.xml').to_return(:status => 200, :body => response_body, :headers => {})
        expect(@subject.template_update '2', 'partial', '_partial.html.liquid').to match_array(['2', anything])
      end

      it 'should update a builtin_page template - and return id and updated at' do
        local_files = make_local_files
        make_files local_files, '.', ['builtin_page.html.liquid']
        updated_at = Time.now
        response_body = xml_header + "<builtin_page><id>2</id><updated_at>#{updated_at}</updated_at><system_name>show</system_name></builtin_page>"
        stub_request(:put, CMS_URL + '/templates/2.xml').to_return(:status => 200, :body => response_body, :headers => {})
        expect(@subject.template_update '2', 'builtin_page', 'builtin_page.html.liquid').to match_array(['2', anything])
      end
    end

    describe '#section_get' do
      it 'should fetch the correct section by id' do
        parameters = '?provider_key=provider_key&id=3'
        updated_at = Time.now
        response_body = xml_header + "<section><id>3</id><updated_at>#{updated_at}</updated_at></section>"
        stub_request(:get, CMS_URL + '/sections/3.xml' + parameters).to_return(:status => 200, :body => response_body, :headers => {})
        id, _ = @subject.section_get '3'
        expect(id).to eq '3'
      end
    end

    describe '#section_create', fakefs: true do
      it 'should create a section - and return id and updated at' do
        local_files = make_local_files
        make_directory local_files, '.', 'subdir', [], []
        updated_at = Time.now
        response_body = xml_header + "<section><id>3</id><updated_at>#{updated_at}</updated_at></section>"
        stub_request(:post, CMS_URL + '/sections.xml').to_return(:status => 201, :body => response_body, :headers => {})
        id, _ = @subject.section_create '/subdir', 'SUbDir', '1'
        expect(id).to eq '3'
      end
    end

    describe '#section_update', fakefs: true do
      it 'should update a section - and return id and updated at' do
        local_files = make_local_files
        make_directory local_files, '.', 'subdir', [], []
        updated_at = Time.now
        response_body = xml_header + "<section><id>2</id><updated_at>#{updated_at}</updated_at></section>"
        stub_request(:put, CMS_URL + '/sections/2.xml').to_return(:status => 200, :body => response_body, :headers => {})
        expect(@subject.section_update '2', 'Subdir', '1', '/subdir').to match_array(['2', anything])
      end
    end
  end
end
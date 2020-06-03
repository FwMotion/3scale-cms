require 'nokogiri'
require 'rest-client'
require_relative 'mapper'

module Threescale
  module CMS
    class Api
      attr_reader :base_url

      def initialize(provider_key, url)
        raise 'invalid provider_key' if !provider_key || provider_key.empty?
        raise "Invalid URL '#{url}' for CMS" unless url =~ URI::regexp
        @provider_key = provider_key
        @base_url = "#{url}/admin/api/cms"
        dev_url = url.gsub('-admin','')
        @dev_url = "#{dev_url}"
      end

      def list(kind, page = 1)
        check_kind_or_raise kind

        cms_list = {}
        response = http_request :get, "#{@base_url}/#{kind.to_s}s.xml", 200,
                                  { :params => {
                                                :per_page => 100,
                                                :page => page,
                                                :provider_key => @provider_key} }

        doc = Nokogiri::XML(response.body)
        doc.xpath("#{kind.to_s}s/*").each do |node|
          key, info = key_and_info_from_cms_node kind, node
          cms_list[key] = info
        end

        total_pages_xpath = doc.xpath('//*[@total_pages]')[0]
        total_pages = total_pages_xpath ? total_pages_xpath.attr('total_pages').to_i : 1

        if page < total_pages
          cms_list.merge! list(kind, page + 1)
        end
        cms_list
      end

      def delete(kind, id)
        check_kind_or_raise kind
        check_id_or_raise id

        http_request :delete, "#{@base_url}/#{kind}s/#{id}.xml", 200,
                     { :params =>
                                  { :provider_key => @provider_key,
                                    :id => id } }
      end

      def file_update(path, id, section_id, filename)
        check_path_or_raise path
        check_id_or_raise id
        check_id_or_raise section_id
        check_filename_or_raise filename

        response = http_request :put, "#{@base_url}/files/#{id}.xml", 200,
                        { :provider_key => @provider_key,
                          :id => id,
                          :path => path,
                          :section_id => section_id,
                          :tag_list => {},
                          :downloadable => 0,
                          :attachment => File.new(filename) }

        parse_response response, 'file'
      end

      def file_create(path, section_id, filename, tag_list)
        check_path_or_raise path
        check_id_or_raise section_id
        check_filename_or_raise filename
        check_taglist_or_raise tag_list

        response = http_request :post, @base_url + '/files.xml', 201,
                                   {
                                    :provider_key => @provider_key,
                                    :path => path,
                                    :section_id => section_id,
                                    :tag_list => tag_list,
                                    :downloadable => 0,
                                    :attachment => File.new(filename) }

        parse_response response, 'file'
      end

      def file_get(id)
        check_id_or_raise id

        response = http_request :get, "#{@base_url}/files/#{id}.xml", 200,
                                { :params => { :provider_key => @provider_key} }
        doc = Nokogiri::XML(response.body)
        url = doc.xpath('//url')[0].text
        if url.start_with?('http') == false
          path = doc.xpath('//path')[0].text
          url = "#{@dev_url}#{path}"
        end
        URI(url)
      end

      # TODO return both draft and published in response and let the application decide
      def template_get(id)
        check_id_or_raise id

        response = http_request :get, "#{@base_url}/templates/#{id}.xml", 200,
                                { :params => { :provider_key => @provider_key } }

        doc = Nokogiri::XML(response.body)
        draft_node = doc.xpath('//draft')[0]
        published_node = doc.xpath('//published')[0]
        draft = draft_node ? draft_node.text : nil
        published = published_node ? published_node.text : nil
        if draft && !draft.empty?
          draft
        else
          published
        end
      end

      # TODO accept draft and/or published content and send accordingly if API accepts them
      def template_update(id, type, filename)
        check_id_or_raise id
        check_template_type_or_raise type
        check_filename_or_raise filename

        response = http_request :put, @base_url + "/templates/#{id}.xml", 200,
                     { :provider_key => @provider_key,
                       :draft => File.read(filename) }
        parse_response response, type
      end

      # TODO accept draft and/or published content and send accordingly if API accepts them
      def template_create(path, section_name, section_id, system_name, title, layout_name, type, filename, liquid)
        check_path_or_raise path
        check_section_name_or_raise section_name
        check_id_or_raise section_id
        check_title_or_raise title
        check_layout_name_or_raise layout_name
        check_template_type_or_raise type
        check_filename_or_raise filename

        params = { :provider_key => @provider_key,
                   :type => type,
                   :title => title,
                   :section_name => section_name,
                   :section_id => section_id,
                   :layout_name => layout_name,
                   :path => path,
                   :liquid_enabled => liquid,
                   :draft => File.read(filename) }

        unless type == 'page'
          check_system_name_or_raise system_name
          params[:system_name] = system_name
        end

        response = http_request :post, @base_url + '/templates.xml', 201, params

        parse_response response, type
      end

      def section_get(id)
        check_id_or_raise id

        response = http_request :get, "#{@base_url}/sections/#{id}.xml", 200,
                                { :params => { :provider_key => @provider_key,
                                  :id => id } }

        parse_response response, 'section'
      end

      def section_update(id, title, parent_id, partial_path)
        check_id_or_raise id
        check_title_or_raise title
        check_id_or_raise parent_id

        response = http_request :put, "#{@base_url}/sections/#{id}.xml", 200,
                                { :provider_key => @provider_key,
                                  :id => id,
                                  :title => title,
                                  :public => 1,
                                  :parent_id => parent_id,
                                  :partial_path => partial_path }

        parse_response response, 'section'
      end

      def section_create(partial_path, title, parent_id)
        check_title_or_raise title
        check_id_or_raise parent_id

        response = http_request :post, "#{@base_url}/sections.xml", 201,
                                { :provider_key => @provider_key,
                                  :title => title,
                                  :public => 1,
                                  :parent_id => parent_id,
                                  :partial_path => partial_path }

        parse_response response, 'section'
      end

      private ####################################### PRIVATE METHODS ######################

      def parse_response(response, node_name)
        doc = Nokogiri::XML(response.body)
        node = doc.xpath("//#{node_name}")[0]
        info_from_node node
      end

      def info_from_node(node)
        id = node.xpath('id').text
        raise "missing property 'id' of CMS entry" if !id || id.empty?

        updated_at =node.xpath('updated_at').text
        raise "missing property 'updated_at' of CMS entry" if !updated_at || updated_at.empty?

        return id, updated_at
      end

      # Gets the property of the node by its name, or raises an exception if it's empty
      def get_property!(node, name)
        value = node.xpath(name).text
        raise "missing property '#{name}' in node: #{node}" if value.empty?
        value
      end

      # Gets the property of the node by its name
      def get_property(node, name)
        node.xpath(name).text
      end

      def key_and_info_from_cms_node(kind, node)
        id, updated_at = info_from_node node

        info = {id: id, kind: kind, updated_at: Time.parse(updated_at)}

        if kind == :file
          key = get_property! node, 'path' # Use the file's path as the key
          info[:section_id] = get_property! node, 'section_id'
          info[:path] = get_property! node, 'path'
          info[:title] = get_property! node, 'title'
        elsif kind == :section
          key = get_property! node, 'partial_path' # Use the section's partial_path as the key
          info[:partial_path] = key
          info[:public] = get_property! node, 'public'
          info[:title] = get_property! node, 'title'
          info[:parent_id] = node.xpath('parent_id').text #root may not have parent_id
          info[:system_name] = get_property! node, 'system_name'
        else # template - actual node names can be of different types
          type = node.name  # subtype such as layout, page, etc
          raise "missing property 'type' for template in CMS" unless type
          info[:type] = type
          info[:liquid_enabled] = (get_property(node, 'liquid_enabled') == 'true')
          case type
            when 'layout'
              key = get_property! node, 'system_name'
              info[:system_name] = key
            when 'partial'
              key = get_property! node, 'system_name'
            when 'page'
              path_node = node.at_xpath('path')
              raise "missing property 'path' in template of type 'page'" unless path_node
              key = path_node.text # pages don't have system name and use path instead
            when 'builtin_page'
              key = get_property! node, 'system_name'
            when 'builtin_partial'
              key = get_property! node, 'system_name'
            else
              raise "Unknown type '#{type}' of template"
          end
        end

        return key, info
      end

# noinspection RubyResolve
      def http_request(method, url, expected_code, options = {})
        response = RestClient::Request.execute(method: method, url: url, headers: options, verify_ssl: false)
        if response.code != expected_code
          raise "Request (#{method}) to url: '#{url}' returned unexpected response code: #{response.code}\n\t#{response.body}"
        end

        doc = Nokogiri::XML(response)
        error = doc.xpath('//error')
        unless error.empty?
          options.delete(:draft)
          options.delete(:attachment)
          options.delete(:template)
          raise "#{error[0].text}\n"
        end

        return response

      rescue RestClient::UnprocessableEntity => e
        options.delete(:draft)
        options.delete(:attachment)
        options.delete(:template)
        doc = Nokogiri::XML(e.response)
        error = doc.xpath('//error')[0] if doc
        error_text = error.text if error
        raise "Error performing #{method.upcase} to url: '#{url}' \n\t\t#{options}\n\t\t#{error_text}\n"
      end

      def check_path_or_raise(path)
        raise "Invalid path '#{path}' as must start with '/'" unless path.start_with? '/'
      end

      def check_kind_or_raise(kind)
        case kind
          when :section
          when :file
          when :template
          else
            raise "Invalid type '#{kind}' for CMS content"
        end
      end

      def check_template_type_or_raise(type)
        raise "Invalid template type '#{type}'" unless Threescale::CMS::Mapper::VALID_TEMPLATE_TYPES.include? type
      end

      # Check it is a valid id, i.e. a positive integer
      def check_id_or_raise(id)
        id_number = Integer(id, 10)
        raise "'id' parameter with value '#{id}' should be a positive integer" if id_number < 0
      end

      def check_filename_or_raise(filename)
        raise "Invalid filename / file-not-found '#{filename}'" unless File.exists? filename
      end

      def check_taglist_or_raise(tag_list)

      end

      def check_section_name_or_raise(section_name)

      end

      def check_system_name_or_raise(system_name)
        raise "Invalid system_name, cannot contain '.' character" if system_name.include? '.'
      end

      def check_title_or_raise(title)

      end

      def check_layout_name_or_raise(layout_name)

      end
    end
  end
end

module Threescale
  module CMS
    class Mapper
      # The purpose of this class is to map 'cms keys' to 'local keys' to 'local paths'
      #
      # 'cms keys' are identifiers of objects in the CMS, and are in fact paths, partial_paths or system_names
      # depending on the object type
      #
      # 'local keys' are used to refer to objects in the cms tool's hash during execution.
      # This hash is created by calling 'fetch_list', which downloads the list of cms contents of
      # type :section, :file, or :template
      #
      # 'local paths' are used to refer to the files locally on the file system
      #
      # This mapping is required in order to handle:
      #    - mapping absolute paths (starting with '/') in the cms to relative paths locally
      #    - a few special cases of naming related to the 'root' section and the 'index' page
      #    - removal/addition of special file type suffixes to some types of content
      #    - removal/addition of special prefixes to some types of content to identify their type on the local file system
      #
      # CMS Contents can be of the following kinds and types and their local prefixes/suffixes
      # section
      #                  - No particular local prefix - will be a directory in the local file system
      #                  - Map 'cms key' of '/' or 'root' onto '.' locally
      # file
      #                  - No particular local prefix, any suffix that is not '.html.liquid'
      #                  - No special cases to handle
      # template
      #    layout        -          'l_' local prefix, suffix is '.html.liquid'
      #    partial       -           '_' local prefix, suffix is '.html.liquid'
      #    page          - No particular local prefix, suffix is '.html.liquid'    <<<< issue!
      #                  - or can be '.css' or '.js' for some reason they are sometimes created as pages not files
      #    builtin_page  - No particular local prefix, suffix is '.html.liquid'    <<<< issue!
      #
      # Root in the CMS commonally has two partial paths associated with it (the default and recommended is 'root')
      ROOT_PARTIAL_PATH = '/'
      ROOT_OTHER_PARTIAL_PATH = 'root'
      # This maps to the local current working directory
      ROOT_LOCAL_KEY = '.'

      # The path '/' in the CMS is used to indicate the index page for the developer portal
      INDEX_PATH = '/'
      # and this maps to the local file with the base name 'index'
      INDEX_NAME = 'index'

      HTML_SUFFIX = '.html'
      LIQUID_SUFFIX = '.liquid'

      # All templates locally have the '.html.liquid' suffix
      TEMPLATE_SUFFIX = HTML_SUFFIX + LIQUID_SUFFIX
      # Partial templates locally have the '_' prefix
      PARTIAL_PREFIX = '_'
      # Layout templates locally have the 'l_' prefix
      LAYOUT_PREFIX = 'l_'

      # Map extensions to their content type
      CONTENT_TYPE_MAPPING = {
          ''     => '',
          'htm'  => 'text/html',
          'html' => 'text/html',
          'txt'  => 'text/plain',
          'png'  => 'image/png',
          'gif'  => 'image/gif',
          'jpg'  => 'image/jpeg',
          'jpeg' => 'image/jpeg',
          'css'  => 'text/css',
          'js'   => 'text/javascript',
          'json' => 'application/json',
          'ico'  => 'image/x-icon'
      }

      # noinspection RubyLiteralArrayInspection
      VALID_TEMPLATE_TYPES = ['layout', 'partial', 'builtin_partial', 'page', 'builtin_page']

      # Treat as binary data if content type cannot be found
      DEFAULT_CONTENT_TYPE = 'application/octet-stream'

      # This helper function parses the extension of the requested file and then looks up its content type.
      def self.content_type(path)
        ext = File.extname(path).split('.').last
        CONTENT_TYPE_MAPPING.fetch(ext, DEFAULT_CONTENT_TYPE)
      end

      def self.local_info_from_requested_path(local_files, path)
        path = "#{INDEX_NAME}" if path == INDEX_PATH

        # Make path relative to CWD
        path = "#{path[1..-1]}" if path.start_with? '/'

        # try all valid extensions
        CONTENT_TYPE_MAPPING.keys.map { |ext|
          filename = path
          filename ="#{filename}.#{ext}" unless ext.empty?
          if local_files.in_list filename
            return filename, :file
          else
            filename = "#{filename}#{LIQUID_SUFFIX}"
            if local_files.in_list filename
              return filename, :template
            end
          end
        }

        raise "No content found with path = #{path}"
      end

      def self.local_key_from_cms_key(key, entry)
        case entry[:kind]
          when :section
            key = ROOT_LOCAL_KEY if key == ROOT_PARTIAL_PATH || key == ROOT_OTHER_PARTIAL_PATH
          when :file
          when :template
            case entry[:type]
              when 'layout'
                key = "#{LAYOUT_PREFIX}#{key}"
              when 'partial', 'builtin_partial'
                key = "#{PARTIAL_PREFIX}#{key}" # TODO is it OK to assume partials are all in root folder??
              when 'page'
                STDERR.puts "\nWarning: page path '#{key}' ends with #{LIQUID_SUFFIX}" if key.end_with? LIQUID_SUFFIX
                STDERR.puts "\nWarning: page path '#{key}' ends with #{HTML_SUFFIX}" if key.end_with? HTML_SUFFIX
                key = INDEX_NAME if key == INDEX_PATH
              when 'builtin_page'
                # TODO check if we detect properly a builtin_page for index path
              else
                raise 'Unknown template type'
            end

            # All pages have ".html" added to the name
            key += HTML_SUFFIX
            # append '.liquid' suffix if this template has liquid processing enabled
            key += LIQUID_SUFFIX if entry[:liquid_enabled]
          else
            raise 'Unknown cms entry kind'
        end

        # remove leading '/' to map to relative local file path
        key.sub(/^\//, '')
      end

      def self.kind_and_type_from_path(path)
        filename = File.basename path
        if File.directory? filename
          kind = :section
        elsif filename.end_with?(LIQUID_SUFFIX) || filename.end_with?(HTML_SUFFIX)
          kind = :template

          if filename.start_with? PARTIAL_PREFIX
            type = 'partial'
          elsif filename.start_with? LAYOUT_PREFIX
            type = 'layout'
          else
            type = 'page'
          end
        else
          kind = :file
        end
        # noinspection RubyScope
        return kind, type
      end

      # noinspection RubyClassMethodNamingConvention
      def self.cms_section_key_from_local_section_key(local_section_key)
        if local_section_key == ROOT_LOCAL_KEY
          ROOT_OTHER_PARTIAL_PATH
        else
          local_section_key
        end
      end

      def self.local_section_key_from_path(path)
        local_section_key = path.split(File::SEPARATOR)[0..-2].join(File::SEPARATOR)
        local_section_key = Mapper::ROOT_LOCAL_KEY if local_section_key.empty?
        local_section_key
      end

      def self.cmsinfo_from_path(local_path)
        liquid = local_path.end_with?(LIQUID_SUFFIX) ? 1 : 0
        # TODO use prefix constants in the regex also....
        cms_path = local_path.gsub(/\.liquid$/, '').gsub(/\.html$/, '').gsub(/^_/, '').gsub(/^l_/, '').gsub(/\/_/, '/').gsub(/\/l_/, '/')
        if cms_path == INDEX_NAME
          cms_path = INDEX_PATH
        else
          cms_path = "/#{cms_path}"
        end

        title = File.basename(cms_path)
        system_name = cms_path.gsub(/^\//, '')

        return title, system_name, cms_path, liquid
      end
    end
  end
end
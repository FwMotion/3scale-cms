#!/usr/bin/env ruby

require 'local_files'
require 'cms_api'
require 'cms'
require 'serve'

module Threescale
  module CMS
    class CLI
      USAGE =
          "Usage:\n"\
          "   cms PROVIDER_KEY URL download             - will download all contents of the specified CMS to the local folder\n"\
          "   cms PROVIDER_KEY URL upload [filename]    - upload all content, specified file or all specified folder contents to CMS\n"\
          "                                             - option: --layout [layout_file_name] to specify layout for new (not updated) pages\n"\
          "   cms PROVIDER_KEY URL delete [filename]    - will delete all possible content in the specified CMS\n"\
          "   cms PROVIDER_KEY URL info [details]       - will display information on CMS and local files\n"\
          "   cms PROVIDER_KEY URL diff [details]       - will display the difference between CMS and local files\n"

      def self.start(argv)
        if argv.size >= 3
          provider_key = argv.shift
          url = argv.shift
          # check url parses OK
          raise "Invalid URL '#{url}' for CMS" unless url =~ URI::regexp

          begin
            action = argv.shift

            cms_client = get_cms_client(provider_key, url, Dir.pwd)
            case action
              when 'delete'
                cms_client.delete argv.shift
              when 'download'
                cms_client.download argv.shift
              when 'upload' # [filename] [--layout layout_name]
                if argv[0] != '--layout'
                  file = argv.shift
                end

                if argv[0] == '--layout'
                  argv.shift
                  layout = argv.shift
                end

                file = '.' if !file || file.empty?
                cms_client.upload file, layout
              when 'info'
                option = argv.shift
                if option && option != 'details'
                  puts "Invalid option '#{option}' for diff command"
                  puts USAGE
                else
                  details = option && option == 'details'
                  cms_client.info details
                end
              when 'diff'
                option = argv.shift
                if option && option != 'details'
                  puts "Invalid option '#{option}' for diff command"
                  puts USAGE
                else
                  details = option && option == 'details'
                  cms_client.diff details
                end
              when 'serve'
                trap('INT') { puts 'Shutting down.'; socket.close; context.terminate; exit }
                server = Threescale::CMS::Server.new(cms_client.local_files)
                server.serve
              else
                puts USAGE
            end

          rescue Exception => e
            STDERR.puts "'#{action}' failed: #{e.message}"
          end
        else
          puts USAGE
        end
      end

      def self.get_cms_client(provider_key, url, root_file)
        STDOUT.sync = true
        puts "\n"
        local_files = Threescale::CMS::LocalFiles.new root_file
        cms_api = Threescale::CMS::Api.new(provider_key, url)
        Threescale::CMS::Cms.new(cms_api, local_files)
      end
    end
  end
end
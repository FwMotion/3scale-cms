require 'fileutils'
require 'openssl'
require_relative 'mapper'

module Threescale
  module CMS
    class LocalFiles
      CMS_IGNORE_FILENAME = '.cmsignore'
      CMS_IGNORE_EXAMPLE_FILENAME = 'cmsignore_example'

      # TODO build a map of local files similar to how the cms_info is built and that can be used to compare
      # but also for serving content, with kind, etc

      def initialize(root)
        @root = root
        @default_layout = nil
      end

      def list
        @list ||= build_file_list(ignore_list, @root)
      end

      def get_default_layout
        list # Make sure files are scanned and file list built first - finding default layout
        @default_layout
      end

      def in_list(path)
        list.include?(path) && !ignore_list.include?(path)
      end

      def directory_entries(filename)
        entries = []
        if File.directory? filename
          Dir.glob("#{filename}/*").each do |folder_entry|
            folder_entry = folder_entry.gsub(/^.\//, '')
            entries << folder_entry unless ignore_list.include? folder_entry
          end
        end
        entries
      end

      def info(details=false)
        info_ignored_files ignore_list, details, CMS_IGNORE_FILENAME
        info_local_files list, details
      end

      def save_template(relative_path, content, updated_at)
        make_directories relative_path

        mtimes = get_ancestor_mtimes relative_path

        if content
          file = File.new(relative_path, 'w')
          file.write(content)
          file.close
        end

        update relative_path, updated_at

        set_mtimes mtimes
      end

      def fetch_and_save_file(relative_path, file_url, updated_at)
        make_directories relative_path

        mtimes = get_ancestor_mtimes relative_path

        http = Net::HTTP.new(file_url.host, file_url.port)
        if file_url.scheme == 'https'
          http.use_ssl = true
          # noinspection RubyResolve
          http.verify_mode = OpenSSL::SSL::VERIFY_NONE
        end

        # Get the File contents from the specified URL in 'request_uri'
        http = http.start
        request = Net::HTTP::Get.new file_url.request_uri
        http.request(request) do |file_response|
          open(relative_path, 'w') do |io|
            file_response.read_body do |chunk|
              io.write(chunk)
            end
          end
        end

        update relative_path, updated_at
        set_mtimes mtimes
      end

      def save_section(relative_path, updated_at)
        make_directories relative_path

        mtimes = get_ancestor_mtimes relative_path
        FileUtils.mkdir_p relative_path
        update relative_path, updated_at
        set_mtimes mtimes
      end

      def needs_update(path, updated_at)
        list.include?(path) && (list[path] < updated_at)
      end

      def needs_creation(path)
        !list.include? path
      end

      def is_newer(path, updated_at)
        list.include?(path) && list[path] > updated_at
      end

      def update(relative_path, updated_at)
        FileUtils.touch relative_path, :mtime => updated_at
        if ignore_list.include? relative_path
          # TODO change to stderr when fakefs fixes it's bugs!
          puts "You have updated the file '#{relative_path}' that is in the ignored files list"
        end
      end

      private

      def make_directories(path)
        dir_path = File.dirname path
        if dir_path && !File.exists?(dir_path)
          mtimes = get_ancestor_mtimes path
          FileUtils.mkdir_p(dir_path)
          set_mtimes mtimes
        end
      end

      def ignore_list
        @ignore_list ||= build_ignore_list(CMS_IGNORE_FILENAME)
      end

      def get_ancestor_mtimes(path)
        # Preserve the modified time of all ancestor directories
        mtimes = {}
        unless path == '.'
          Pathname.new(path).dirname.descend { |directory|
            mtimes[directory.to_s] = directory.mtime if directory.exist?
          }
        end
        mtimes
      end

      def set_mtimes(mtimes)
        mtimes.each do |name, mtime|
          FileUtils.touch name, :mtime => mtime
        end
      end

      def build_ignore_list(ignore_file)
        unless File.exists?(ignore_file)
          puts 'No ignore file found'
          example = File.join(File.dirname(__FILE__), CMS_IGNORE_EXAMPLE_FILENAME)
          FileUtils.copy example, CMS_IGNORE_FILENAME
          puts "Created ignore file at '#{File.expand_path(ignore_file)}' from example"
        end

        puts "Loaded ignore file list from '#{File.expand_path(ignore_file)}'"
        File.readlines(ignore_file).map(&:strip).flat_map(&Dir.method(:glob))
      end

      def build_file_list(ignore_list, root)
        puts "Analyzing local files under '#{root}'"
        file_list = {}
        file_list[Mapper::ROOT_LOCAL_KEY] = File.mtime(root) # Add root explicitly
        # Dir.glob("#{root}/**/*").each { |file|        
        Dir.glob('**/*').each { |file|
          file_list[File::path(file)] = File.mtime(file) unless ignore_list.include? file
          if File.basename(file).start_with? Mapper::LAYOUT_PREFIX
            @default_layout ||= file
          end
        }
        file_list
      end

      def info_local_files(list, details)
        puts "#{list.length} (non-ignored) local files"
        if details
          list.each do |filename, _|
            filename = "#{filename}/" if File.directory? filename
            puts "\t'#{filename}'"
          end
          puts "\n"
        end
      end

      def info_ignored_files(ignore_list, details, ignore_filename)
        if File.exist?(ignore_filename)
          puts "#{ignore_list.length } ignored local files"
          if details
            ignore_list.each do |filename|
              filename = "#{filename}/" if File.directory?(filename)
              puts "\t'#{filename}'"
            end
            puts "\n"
          end
        end
      end
    end
  end
end

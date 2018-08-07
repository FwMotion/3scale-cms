require 'time'
require 'pathname'
require_relative 'mapper'

module Threescale
  module CMS
    class Cms
      attr_reader :local_files

      def initialize(cms_api, local_files)
        @cms_api = cms_api
        @local_files = local_files
        @implicit_folder_list = {}
      end

      def info(details)
        remote_info details
        @local_files.info details
      end

      def delete(filename = nil)
        if filename
            delete_by_filename filename
          else
            delete_all_cms_entries
        end
      end

      def download(filename = nil)
        if filename
          get_by_filename(filename)
        else
          to_update, to_create = download_list list
          to_update.each do |key|
            get_from_cms key, list[key]
          end
          to_create.each do |key|
            get_from_cms key, list[key]
          end
        end
      end

      def upload(filename, layout=nil)
        if @local_files.in_list filename
          cms_info = list[filename]
          if cms_info
            update_in_cms filename, cms_info, filename, layout
          else
            create_in_cms filename, layout
          end
        else
          STDERR.puts "File '#{filename}' not found or ignored"
        end
      end

      def diff(details)
        to_create_locally, to_update_locally = download_list list
        puts "\nFiles that will be created locally on 'download'" if details && !to_create_locally.empty?
        to_create_locally.each do |key|
          puts "'#{key}'" if details
        end

        puts "\nFiles that exist locally that will be updated on 'download'" if details && !to_update_locally.empty?
        to_update_locally.each do |key|
          puts "'#{key}'" if details
        end

        to_create_on_cms, to_update_on_cms = upload_list list, @local_files.list, @implicit_folder_list

        puts "\nFiles that exist locally will be created in CMS on 'upload'" if details && !to_create_on_cms.empty?
        to_create_on_cms.each do |path|
          puts "'#{path}'" if details
        end

        puts "\nFiles that have been modified locally and will be updated in CMS on 'upload'" if details && !to_update_on_cms.empty?
        to_update_on_cms.each do |key|
          puts "'#{key}'" if details
        end

        puts "\nSummary:"
        puts "#{to_create_locally.length} files to be created locally"
        puts "#{to_update_locally.length} files to be updated locally"
        puts "#{to_create_on_cms.length} files to be created on CMS"
        puts "#{to_update_on_cms.length} files to be updated on CMS"
      end

      private ########################################

      def list
        @list ||= fetch_list
      end

      def fetch_list
        puts "Getting CMS content from #{@cms_api.base_url} "
        # NOTE: It is important that the list of sections is downloaded before the list of files, so that we can
        # detect files that create subfolders (due to having a '/' in their system name) and avoid considering those folders as sections
        # NOTE: normalize the keys to avoid clashes between things like root section (path = '/') and index (path = '/' also).
        # as they will map to '.' and 'index.html.liquid' and the keys won't clash.
        cms_list = normalise_keys @cms_api.list(:section)
        cms_list.merge! normalise_keys @cms_api.list(:file)
        cms_list.merge! normalise_keys @cms_api.list(:template).sort_by{ |_, v| v[:type] }.to_h

        @implicit_folder_list = build_implicit_folders_list cms_list

        cms_list
      end

      def default_layout_name(layout)
        @default_layout_name ||= find_default_layout_name layout
      end

      def normalise_keys(cms_list)
        new_list = {}

        cms_list.each do |key, entry|
          new_key = Mapper::local_key_from_cms_key key, entry
          new_list[new_key] = entry
        end

        new_list
      end

      # For uploading pages we want to select a default layout to apply to it, which can later be overridden by the user
      # in the CMS UI. This is to allow automated upload of new pages found locally - where we have no idea of layout to us
      def find_default_layout_name(default_layout)
        default_layout_path = nil
        if default_layout
          layout_entries = list.detect { | cms_entry |
            cms_entry[1][:system_name] == default_layout
          }
        else
          layout_entries = list.detect { | cms_entry |
            cms_entry[1][:kind] == :template && cms_entry[1][:type] == 'layout'
          }
        end
        default_layout_path = layout_entries[0] if layout_entries

        # if we didn't find an existing layout to use in the CMS list, then let's try and find one locally we can upload first
        unless default_layout_path
          default_layout_path = @local_files.get_default_layout
          if default_layout_path
            puts "Uploading layout '#{default_layout_path}' to be used as default layout for pages"
            create_file_or_template_in_cms default_layout_path, nil
          end
        end

        raise "Could not find a default layout to use for new pages.\nTry uploading a layout first using 'upload $layout_filename'" unless default_layout_path
        _, default_layout_name, _, _ = Mapper::cmsinfo_from_path default_layout_path
        puts "The layout '#{default_layout_name}' in file '#{default_layout_path}' was selected as the default layout for uploading new pages"
        default_layout_name
      end

      def info_implicit_folders(implicit_folder_list, details)
        puts "#{implicit_folder_list.length} implicit folders due to file/template system_names containing '/'"
        if details
          implicit_folder_list.each do |folder, _|
            puts "\t '#{folder}/'"
          end
          puts "\n"
        end
      end

      # Some keys are system_names and they can have a '/' in them that causes creation of folders that are not
      # sections in the CMS. Track the list of these folders so we can avoid false negatives on diff and upload
      def build_implicit_folders_list(cms_list)
        folder_list = {}
        STDERR.puts "\n"
        cms_list.each do |key, entry|
          if key.include?(File::SEPARATOR)
            Pathname.new(key).dirname.descend { |directory|
              directory_name = directory.to_s

              if directory_name =~ /[A-Z]/
                STDERR.puts "Warning: The #{entry[:kind]} path '#{key}' with title: '#{entry[:title]}' contains uppercase characters. Reccomend to avoid using them in CMS 'path' and 'system_names'"
              end

              unless cms_list[directory_name]
                folder_list[directory_name] = directory_name
                STDERR.puts "Warning: The #{entry[:kind]} in CMS '#{key}' with title: '#{entry[:title]}' includes a folder '#{directory_name}' that is not a section in the CMS" unless entry[:kind] == :template && entry[:type] == 'partial'
              end
            }
          end
        end
        folder_list
      end

      def remote_info(details)
        puts "#{list.size} items found in CMS"

        if details
          list.each do |name, info|
            puts "\t'#{name}' \t#{info[:kind]}"
          end
          puts "\n"
        end

        info_implicit_folders @implicit_folder_list, details
      end

      def delete_by_filename(filename)
        begin
          # Delete the contents of section - depth first - all the way down first
          # as otherwise deleting a section moves the orphaned content to be under root
          @local_files.directory_entries(filename).each do |entry|
            delete_by_filename entry
          end

          cms_entry = list[filename]
          delete_one filename, cms_entry if cms_entry

          rescue Exception => e
            STDERR.puts "delete of '#{cms_entry[:kind]}' '#{filename}' from CMS failed: \n\t#{e.message}"
        end
      end

      def delete_all_cms_entries
        list.each do |key, cms_entry|
            delete_one key, cms_entry unless key == Mapper::ROOT_LOCAL_KEY
        end
      end

      def delete_one(key, cms_entry)
        kind = cms_entry[:kind]
        id = cms_entry[:id]
        type = cms_entry[:type]
        if kind == :template && (type == 'builtin_page' || type == 'builtin_partial')
          puts "Skipping deletion of '#{key}' as it's a builtin and cannot be deleted"
        elsif kind == :section && key == Mapper::ROOT_LOCAL_KEY
        else
          @cms_api.delete kind, id
          puts "Deleted #{kind} '#{key}' with 'id=#{id}' from the CMS"
        end

      rescue Exception => e
        STDERR.puts "delete of '#{kind}' '#{key}' from CMS failed: \t#{e.message}"
      end

      def get_by_filename(local_path)
        if @implicit_folder_list[local_path]
          puts "#{local_path}/ is an implicit folder that is not a section in the CMS, looking at it's contents"
        else
          cms_info = list[local_path]
          raise "'#{local_path}' was not found in the list of files in the CMS to download" unless cms_info
          get_from_cms local_path, cms_info if @local_files.needs_update(local_path, cms_info[:updated_at]) || @local_files.needs_creation(local_path)
        end

        @local_files.directory_entries(local_path).each do |entry|
          get_by_filename entry
        end
      end

      def get_from_cms(cms_key, cms_info)
        id = cms_info[:id]
        kind = cms_info[:kind]
        updated_at = cms_info[:updated_at]

        if kind == :file
          file_url = @cms_api.file_get(id)
          @local_files.fetch_and_save_file(cms_key, file_url, updated_at)
        elsif kind == :section
          @local_files.save_section(cms_key, updated_at)
        else
          content = @cms_api.template_get id
          @local_files.save_template(cms_key, content, updated_at)
        end

        puts "Saved #{kind} '#{cms_key}' with timestamp set to #{updated_at}"
      end

      def download_list(cms_list)
        to_create = []
        to_update = []

        cms_list.each do |key, info|
          to_update << key if @local_files.needs_update key, info[:updated_at]
          to_create << key if @local_files.needs_creation key
        end

        return to_create, to_update
      end

      def upload_list(cms_list, file_list, implicit_folder_list)
        to_create = []
        to_update = []

        cms_list.each do |key, info|
          to_update << key if @local_files.is_newer key, info[:updated_at]
        end

        file_list.each do |local_path, _|
          to_create << local_path unless implicit_folder_list[local_path] || list[local_path]
        end

        return to_create, to_update
      end

      def section_id_from_name(cmslist, section_name)
        cmslist.detect { | cms_entry |
          cms_entry[1][:kind] == :section && cms_entry[0] == section_name
        }
      end

      def section_info_from_path(path)
        local_section_key = Mapper::local_section_key_from_path(path)
        local_section_key = Mapper::ROOT_LOCAL_KEY if @implicit_folder_list[local_section_key]
        sectionid = (section_id_from_name list, local_section_key)[1][:id]
        return Mapper::cms_section_key_from_local_section_key(local_section_key), sectionid
      end

      def update_in_cms(cmslist_key, cmslist_entry, filename, layout)
        section_name, section_id = section_info_from_path filename
        kind = cmslist_entry[:kind]
        if kind == :file
          if @local_files.is_newer filename, cmslist_entry[:updated_at]
            update_file_in_cms(cmslist_key, cmslist_entry, section_id, section_name, filename)
          end
        elsif kind == :section
          if @local_files.is_newer filename, cmslist_entry[:updated_at]
            update_section_in_cms(cmslist_key, cmslist_entry, section_id, filename)
          end

          @local_files.directory_entries(filename).each do |entry|
            upload entry, layout
          end
        else
          if @local_files.is_newer filename, cmslist_entry[:updated_at]
            update_template_in_cms(cmslist_key, cmslist_entry, section_name, filename)
          end
        end

      rescue Exception => e
        STDERR.puts "Failed to update '#{cmslist_key}': #{e.message}"
      end

      def create_in_cms(path, layout)
        create_required_sections_in_cms path

        # TODO if we create the local file list using the specified file/dir then we shouldn't have to do this
        # TODO recursion on the file system, just upload the list - ordering it first: sections, layouts, the rest
        # try to recurse down on contents - sub-directories and files - and either update or create
        if File.directory?(path)
          @local_files.directory_entries(path).each do |entry|
            upload entry, layout
          end
        else
          create_file_or_template_in_cms(path, layout) if File.file? path
        end

      rescue Exception => e
        puts "Failed to create '#{path}': #{e.message}"
      end

      def create_required_sections_in_cms(path)
        parent_id = list[Mapper::ROOT_LOCAL_KEY][:id] # in case of implicit folder - then assume file is in the root section
        dir = File.directory?(path) ? Pathname.new(path) : Pathname.new(path).dirname
        dir.descend { |directory|
          directory_name = directory.to_s
          begin
            cmsinfo = list[directory_name]
            if cmsinfo
              parent_id = cmsinfo[:id]
            else
              unless @implicit_folder_list[directory_name]
                id = create_section_in_cms directory_name, parent_id
                parent_id = id
              end
            end

          rescue Exception => e
            raise "Failed to create required section '#{directory_name}' for '#{path}' : #{e.message}"
          end
        }
      end

      def create_file_or_template_in_cms(local_path, layout)
        section_name, section_id = section_info_from_path local_path
        kind, type = Mapper::kind_and_type_from_path local_path
        title, system_name, cms_path, liquid = Mapper::cmsinfo_from_path local_path
        if kind == :file
          id, updated_at = @cms_api.file_create cms_path, section_id, local_path, {}
        else
          layout_name = nil
          layout_name = default_layout_name(layout) if type == 'page'
          id, updated_at = @cms_api.template_create cms_path, section_name, section_id, system_name, title, layout_name, type, local_path, liquid
        end

        list[local_path] = { kind: kind, id: id, type: type, title: title, system_name: system_name, updated_at: updated_at }
        @local_files.update local_path, Time.parse(updated_at)
        puts "Created #{kind} '#{cms_path}' in CMS with id='#{id}' in section '#{section_name}', timestamp '#{updated_at}'"
      end

      def create_section_in_cms(path, parent_id)
        title = path.split(File::SEPARATOR)[-1]
        id, updated_at = @cms_api.section_create "/#{path}", title, parent_id
        list[path] = { kind: :section, id: id, title: title, updated_at: updated_at }
        @local_files.update path, Time.parse(updated_at)
        puts "Created section '/#{path}' in CMS with title: '#{title}' under parent with id: '#{parent_id}'"
        id
      end

      def update_file_in_cms(cmslist_key, cmslist_entry, section_id, section_name, filename)
        cmslist_key = "/#{cmslist_key}" unless cmslist_key.start_with? '/'
        _, updated_at = @cms_api.file_update cmslist_key, cmslist_entry[:id], section_id, filename
        @local_files.update filename, Time.parse(updated_at)
        puts "Updated file '#{cmslist_key}' in section '#{section_name}' in CMS"
      end

      def update_template_in_cms(cmslist_key, cmslist_entry, section_name, filename)
        _, updated_at = @cms_api.template_update cmslist_entry[:id], cmslist_entry[:type], filename
        @local_files.update filename, Time.parse(updated_at)
        puts "Updated template '#{cmslist_key}' of type '#{cmslist_entry[:type]}' in section '#{section_name}' in CMS"
      end

      def update_section_in_cms(cmslist_key, cms_entry, parent_id, filename)
        _, updated_at = @cms_api.section_update(cms_entry[:id], cms_entry[:title], parent_id, cms_entry[:partial_path])
        @local_files.update filename, Time.parse(updated_at)
        puts "Updated section '#{cmslist_key}' in CMS, timestamp set to '#{updated_at}'"
      end
    end
  end
end

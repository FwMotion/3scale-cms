require 'socket'
require 'uri'
require_relative 'local_files'
require 'liquid'

module Threescale
  module CMS
    class Server
      def initialize(local_files)
        @local_files = local_files
      end

      def get_content(path, kind)
        if kind == :template
          content = Liquid::Template.parse(File.read(path)).render
          content_type = Mapper::content_type(path.gsub(/.liquid/,''))
        else
          content = File.read path
          content_type = Mapper::content_type(path)
        end

        return content, content_type
      end

      def get_requested_path(socket)
        requested = socket.gets
        if requested
          request_uri  = requested.split(' ')[1]
          URI.unescape(URI(request_uri).path)
        end
      end

      def output_headers(socket, content_type, size)
        socket.print "HTTP/1.1 200 OK\r\n" +
                         "Content-Type: #{content_type}\r\n" +
                         "Content-Length: #{size}\r\n" +
                         "Connection: close\r\n"
        socket.print "\r\n"
      end

      def serve_path(socket, path, kind)
        content, content_type = get_content path, kind
        output_headers socket, content_type, content.size
        socket.write(content)
      end

      def start_server
        server = TCPServer.new('localhost', 0)
        puts "Local CMS files are being served from http://localhost:#{server.addr[1]}"
        Liquid::Template.file_system= Liquid::LocalFileSystem.new('./', '_%s.html.liquid'.freeze)
        server
      end

      def serve_requests(server)
        loop do
          begin
            socket = server.accept
            requested_path = get_requested_path socket
            local_path, kind = Mapper::local_info_from_requested_path(@local_files, requested_path)
            serve_path socket, local_path, kind
            STDOUT.puts "Requested #{requested_path}, mapped to #{local_path}"

          rescue Exception => e
            STDERR.puts e.message
            socket.close
          end
        end
      end

      def serve
        server = start_server
        serve_requests server
      end
    end
  end
end

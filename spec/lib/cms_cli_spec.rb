require_relative '../../cms_cli'

describe Threescale::CMS::CLI do
  context 'insufficient number of arguments used' do
    describe '#start' do
      it 'should show usage if no arguments passed' do
        expect { Threescale::CMS::CLI.start [] }.to output(/Usage/).to_stdout
      end

      it 'should show usage if only one argument passed' do
        expect { Threescale::CMS::CLI.start ['x'] }.to output(/Usage/).to_stdout
      end

      it 'should show usage if only two arguments passed' do
        # noinspection RubyLiteralArrayInspection
        expect { Threescale::CMS::CLI.start ['x', 'y'] }.to output(/Usage/).to_stdout
      end
    end
  end

  context 'invalid argument used' do
    describe '#start' do
      it 'should complain about invalid url format' do
        expect { Threescale::CMS::CLI.start ['x', 'y', 'download'] }.to raise_exception(/Invalid URL/)
      end

      it 'should print usage if unknown action specified' do
        expect { Threescale::CMS::CLI.start ['x', 'https://test.com', 'invalid'] }.to output(/Usage/).to_stdout
      end

      it 'should print usage if details is not the extra argument to diff' do
        expect { Threescale::CMS::CLI.start ['x', 'https://test.com', 'diff', 'invalid'] }.to output(/Usage/).to_stdout
      end

      it 'should print usage if details is not the extra argument to info' do
        expect { Threescale::CMS::CLI.start ['x', 'https://test.com', 'info', 'invalid'] }.to output(/Usage/).to_stdout
      end
    end
  end

  context 'valid invocation used' do
    let(:client) { double }
    before {
      allow(Threescale::CMS::CLI).to receive(:get_cms_client).with(anything, anything, anything).and_return client
    }

    describe '#start' do
      it 'should call download with nil when no file/folder specified' do
        expect(client).to receive(:download).with(nil)
        Threescale::CMS::CLI.start ['x', 'https://test.com', 'download']
      end

      it 'should call delete with nil when no file/folder specified' do
        expect(client).to receive(:delete).with(nil)
        Threescale::CMS::CLI.start ['x', 'https://test.com', 'delete']
      end
    end

    it "should call upload with '.' when no file/folder specified" do
      expect(client).to receive(:upload).with('.', nil)
      Threescale::CMS::CLI.start ['x', 'https://test.com', 'upload']
    end

    it "should call upload with 'filename' when file is specified" do
      expect(client).to receive(:upload).with('test-file', nil)
      Threescale::CMS::CLI.start ['x', 'https://test.com', 'upload', 'test-file']
    end

    it "should call upload with '.', '-test-layout' when no file/folder specified, but layout is" do
      expect(client).to receive(:upload).with('.', 'test-layout')
      Threescale::CMS::CLI.start ['x', 'https://test.com', 'upload', '--layout', 'test-layout']
    end

    it "should call upload with 'filename', 'layout' when file and layout is specified" do
      expect(client).to receive(:upload).with('test-file', 'test-layout')
      Threescale::CMS::CLI.start ['x', 'https://test.com', 'upload', 'test-file', '--layout', 'test-layout']
    end

    it 'should call info' do
      expect(client).to receive(:info)
      Threescale::CMS::CLI.start ['x', 'https://test.com', 'info']
    end

    it 'should call diff' do
      expect(client).to receive(:diff)
      Threescale::CMS::CLI.start ['x', 'https://test.com', 'diff']
    end
  end
end

describe 'invalid extra arguments passed' do
  context 'cms commands' do
    let(:client) { double }
    before {
      allow(Threescale::CMS::CLI).to receive(:get_cms_client).with(anything, anything, anything).and_return client
    }

    it 'ignores extra arguments and delete command called with filename' do
      expect(client).to receive(:delete).with('filename')
      # noinspection RubyLiteralArrayInspection
      Threescale::CMS::CLI.start ['x', 'https://test.com', 'delete', 'filename', 'extra']
    end

    it 'ignores extra arguments and download command called with filename' do
      expect(client).to receive(:download).with('filename')
      # noinspection RubyLiteralArrayInspection
      Threescale::CMS::CLI.start ['x', 'https://test.com', 'download', 'filename', 'extra']
    end

    it 'ignores extra arguments and info command called with details' do
      expect(client).to receive(:info).with(true)
      # noinspection RubyLiteralArrayInspection
      Threescale::CMS::CLI.start ['x', 'https://test.com', 'info', 'details', 'extra']
    end

    it 'ignores extra arguments and diff command called with details' do
      expect(client).to receive(:diff).with(true)
      Threescale::CMS::CLI.start ['x', 'https://test.com', 'diff', 'details', 'extra']
    end

    it 'reports an error if the called command raises an exception' do
      allow(client).to receive(:upload).with(anything, anything).and_raise('some exception')
      expect { Threescale::CMS::CLI.start ['x', 'https://test.com', 'upload'] }.to output(/failed/).to_stderr_from_any_process
    end

    # 'upload' with file/dir name plus invalid extras
    # 'upload' with layout option and layout plus invalid extras
  end

  context 'serve command' do
    let(:server) { double }
    before {
      allow(Threescale::CMS::Server).to receive(:new).with(anything).and_return server
    }

    it 'ignores extra arguments and serve called without parameters' do
      expect(server).to receive(:serve)
      Threescale::CMS::CLI.start ['x', 'https://test.com', 'serve', 'extra']
    end

    it 'should call serve' do
      expect(server).to receive(:serve)
      Threescale::CMS::CLI.start ['x', 'https://test.com', 'serve']
    end
  end
end
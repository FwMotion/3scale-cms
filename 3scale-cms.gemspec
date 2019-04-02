Gem::Specification.new do |s|
  s.name        = '3scale-cms'
  s.version     = '0.1.2'
  s.licenses    = ['MIT']
  s.summary     = 'A command line tool for 3scale developer portal CMS'
  s.description = '3scale_cms provides a simple way to work with the developer portal assets locally:
                  download them from the CMS, upload the changes and create new files and templates.'

  s.add_runtime_dependency 'nokogiri', '~> 1.8', '>= 1.8.2'
  s.add_runtime_dependency 'rest-client', '~> 2.0.2', '>= 1.8.0'
  s.add_runtime_dependency 'liquid', '~> 3.0', '>= 3.0.6'

  s.required_ruby_version = '>= 2.0'
  s.require_paths = ['.']
  s.authors     = ['Jakub Hozak', 'Jose Galisteo', 'Maria Pilar Guerra Arias', 'Andrew Mackenzie', 'Daria Mayorova']
  s.email       = 'support@3scale.net'
  s.files       = %w(cms.rb cmsignore_example cms_api.rb cms_cli.rb local_files.rb mapper.rb serve.rb)
  s.executables << 'cms'
end

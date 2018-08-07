
def make_local_files
  wd = '/tmp'
  lf = double 'local_files'
  stub_const('Threescale::CMS::LocalFiles::TEMPLATE_SUFFIX','.html.liquid')
  stub_const('Threescale::CMS::LocalFiles::PARTIAL_PREFIX','_')
  stub_const('Threescale::CMS::LocalFiles::LAYOUT_PREFIX','l_')
  # There is a problem with paths in fakefs if working from '/' https://github.com/fakefs/fakefs/issues/339
  Dir.mkdir wd
  Dir.chdir wd
  allow(lf).to receive(:in_list).with('.').and_return(true) # root exists
  allow(lf).to receive(:is_newer).with('.', anything).and_return(false) # root is older than cms
  allow(lf).to receive(:directory_entries).and_return([]) # FIXME implementation!
  lf
end

def make_directory(lf, parent_directory, directory_path, subdirs, files)
  if parent_directory == '.'
    full_path = directory_path
    else
    full_path = parent_directory + '/' + directory_path
  end
  FileUtils.mkdir_p full_path #TODO remove need for this when cms stops looking at files
  allow(lf).to receive(:directory_entries).with(parent_directory).and_return([full_path])
  allow(lf).to receive(:in_list).with(full_path).and_return(true)
  allow(lf).to receive(:is_newer).with(full_path, anything)
  allow(lf).to receive(:update).with(full_path, anything)

  subdir_paths = []
  subdirs.each do |subdir|
    subdir_path = full_path + '/' + subdir
    subdir_paths.push subdir_path
    make_directory lf, full_path, subdir, [], []
  end

  file_paths = []
  files.each do |file|
    file_path = full_path + '/' + file
    file_paths.push file_path
    make_file lf, file_path
  end

  directory_entries lf, directory_path, subdir_paths + file_paths
end

def directory_entries(lf, directory_path, file_paths)
  allow(lf).to receive(:directory_entries).with(directory_path).and_return(file_paths)
end

def make_files(lf, directory, file_list)
  file_list.each do |file|
    make_file lf, file
  end
  directory_entries lf, directory, file_list
end

# Pass `newer` value if needed to create a file that is newer/older, than in CMS
def make_file(lf, file_path, newer=true)
  FileUtils.touch file_path #TODO remove need for this when cms stops looking at files
  allow(lf).to receive(:in_list).with(file_path).and_return(true)
  allow(lf).to receive(:is_newer).with(file_path, anything).and_return(newer)
  allow(lf).to receive(:update).with(file_path, anything)
end
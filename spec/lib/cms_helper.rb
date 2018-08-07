root_path = 'root'
root_id = '1'
root_title = 'root'
root_system_name = 'root'

ROOT_SECTION =    {
                  :partial_path => root_path,
                  :kind => :section,
                  :id => root_id,
                  :title => root_title,
                  :system_name => root_system_name,
                  :public => 'true',
                  :updated_at => Time.now
                  }

LAYOUT_TEMPLATE = {
                  :path => '/main_layout',
                  :kind => :template,
                  :id => '3',
                  :type => 'layout',
                  :liquid_enabled => true
                  }

PARTIAL_TEMPLATE = {
                  :system_name => 'partial',
                  :kind => :template,
                  :type => 'partial',
                  :id => '4',
                  :liquid_enabled => true
                  }

INDEX_PAGE_TEMPLATE =      {
                  :path => '/',
                  :kind => :template,
                  :type => 'page',
                  :id   => '123',
                  :liquid_enabled => true
                  }

LIQUID_PAGE = 'liquid_page'
LIQUID_PAGE_TEMPLATE =      {
                  :path => "/#{LIQUID_PAGE}",
                  :kind => :template,
                  :type => 'page',
                  :id   => '124',
                  :liquid_enabled => true
                  }

NON_LIQUID_PAGE = 'non_liquid_page'
NON_LIQUID_PAGE_TEMPLATE =      {
                  :path => "/#{NON_LIQUID_PAGE}",
                  :kind => :template,
                  :type => 'page',
                  :id => '125',
                  :liquid_enabled => false
              }

MAIN_LAYOUT_NAME = 'main_layout'

NO_INDEX_CMS = {
    :FILES     => {},
    :SECTIONS  => {
        'root'  => ROOT_SECTION
    },
    :TEMPLATES => {
        MAIN_LAYOUT_NAME  => LAYOUT_TEMPLATE, # NOTE: Keep this after OTHER_PAGE to test ordering
    }
}

NO_LAYOUTS_CMS = {
    :FILES     => {},
    :SECTIONS  => {
                    'root'  => ROOT_SECTION
                  },
    :TEMPLATES => {
                    'index' => INDEX_PAGE_TEMPLATE
                  }
    }

FILES = { 'a_file.jpg' => {
                            :path => '/a_file.jpg',
                            :kind => :file,
                            :id => '129',
                            :title => 'A File.jpg',
                            :section_id => '1',
                            :updated_at => Time.now
                        },
        }

ROOT_SECTION_ONLY = {
    'root'  => ROOT_SECTION
}

ROOT_AND_SUBDIR = {
            'root'  => ROOT_SECTION,
            'directory' => {
                :partial_path => '/directory',
                :kind => :section,
                :title => 'Directory',
                :system_name => 'directory',
                :public => 'true',
                :id => '2',
            }
}

INDEX_ONLY = {
    'index'           => INDEX_PAGE_TEMPLATE
}

TEMPLATES = {
            'index'           => INDEX_PAGE_TEMPLATE,
            "#{LIQUID_PAGE}"  => LIQUID_PAGE_TEMPLATE,
            'non_liquid_page' => NON_LIQUID_PAGE_TEMPLATE,
            # These are files that are treated as templates in CMS (hence editable) as some developer portals do this
            # the liquid processing tag is disabled for them to avoid the name being appending with .liquid
            'style.css'  => {
                :path => '/style.css',
                :kind => :template,
                :type => 'page',
                :id => '126',
                :liquid_enabled => false
            },
            'script.js'  => {
                :path => '/script.js',
                :kind => :template,
                :type => 'page',
                :id => '127',
                :liquid_enabled => false
            },
            'show'         => {
                :system_name => 'show',
                :kind => :template,
                :type => 'builtin_page',
                :id => '128',
                :liquid_enabled => true
            },
            MAIN_LAYOUT_NAME  => LAYOUT_TEMPLATE, # NOTE: Keep this after OTHER_PAGE to test ordering
            'partial'         => PARTIAL_TEMPLATE
        }

BASE_CMS = {
    :FILES     => FILES,
    :SECTIONS  => ROOT_AND_SUBDIR,
    :TEMPLATES => TEMPLATES
    }

IMPLICIT_FOLDER_CMS = {}
IMPLICIT_FOLDER_CMS[:FILES] = FILES.clone
IMPLICIT_FOLDER_CMS[:SECTIONS] = ROOT_AND_SUBDIR.clone
IMPLICIT_FOLDER_CMS[:TEMPLATES] = TEMPLATES.clone
IMPLICIT_FOLDER_CMS[:FILES]['implicit_folder/file.jpg'] =  {
    :path => '/implicit_folder/file.jpg',
    :kind => :file,
    :title => 'File.jpg',
    :section_id => '1'
}

def make_cms(cms)
  cms_api = double 'cms_api'
  allow(cms_api).to receive(:base_url)
  allow(cms_api).to receive(:list).with(:section).and_return(cms[:SECTIONS])
  allow(cms_api).to receive(:list).with(:file).and_return(cms[:FILES])
  allow(cms_api).to receive(:list).with(:template).and_return(cms[:TEMPLATES])
  cms_api
end
# 3scale CMS Tools

**TODO**: Rewrite this as needed.

**Note**: There are plans/work to implement an API for Portal CMS.
Ref: https://issues.redhat.com/browse/THREESCALE-7244

## DISCLAIMER

It is important to know that these tools, as well as the 3scale REST API that
they use, are undocumented and not supported by Red Hat. These CMS tools, as
well as the underlying API, may change at any moment, without any guarantee
of backwards compatibility. Therefore, we cannot recommend this tool for
production use.

## Concepts

### File

A file is a binary item, such as an image.

### Section

Section is a logical grouping, somewhat similar to a directory.

### Template

Templates refer to textual content that will be served from the 3scale CMS.
Templates may hold static content (such as scripts or stylesheets) or may be
templated for server-side rendering as needed.

## Installation

Installation is performed using a container host, such as podman:

```bash
podman pull ghcr.io/fwmotion/3scale-cms
```

## Usage Overview

The `3scale-cms` command enables you to do offline editing, changes or
version control of the contents of a CMS in your admin portal in 3scale.

In the CMS it is possible to create a file, a template or a section. Files are
for example an image, a JS script or a CSS stylesheet. A template is generally
content in `.html.liquid` file. A section is a hierarchical folder in the CMS
for storing other elements.

### Mirroring CMS contents locally

The mirror used locally is a hierarchy of folders that mirrors the content organization in the CMS. Thus _sections_ in the CMS are mirrored as directories on your local file system, and the elements below that section in the CMS are placed inside that directory.

An exception occurs, as it is possible to create a file/template in the CMS that is served from a path other than its location in the CMS. e.g. a file called `image.jpg` that is in the root section of the CMS, but is served from `other_path/image.jpg`. This file will be mirrored locally into `./other_path/image.jpg`, with the directory `other_path` being created to store it. However, this directory is tracked as one of the _implicit folders_, to avoid a section for it being created by mistake on any later upload.

### CMS Ignore File

It is often desireable to have some files in the local directory that you do not want to upload to the CMS. Examples could be files used in the version control of your CMS contents (e.g. a `.git` folder), or files used in the testing or Continuous Integration of your contents (e.g. `travis.yml` file).

To have the `3scale-cms` command ignore these files, they can be added to the `.cmsignore` file in the root directory of the CMS mirror. These files use the 'glob' format to allow specifying patterns of files and directories, not just specific files.

### New local page/layout files

When a local file of type `.html` or `.html.liquid` is created that does not have the '_' prefix to indicate it is a partial, then the tool cannot tell if it is intended as a new page or new layout.

To allow unattended use of the tool, at the moment it assumes that new pages are more frequent and more useful and so assumes the new file is a page and uploads it as such.

### Default layouts for new pages

When a page is created in the CMS, the layout to apply to it must be specified. To allow for automatic use of the tool
without user intervention the tool chooses a default layout from the layouts in the CMS to use for new files it creates.

Upon start-up the tool examines the list of layouts in the CMS and choses one as the default layout for new pages that
will be created.

If no layouts are available in the CMS, the tool will not run.

If you wish to use a different layout for a newly created page, you currently have to go to the CMS in the admin portal
and change it manually.

## Using the `3scale-cms` command

### Prerequisites

You must have an account in 3scale. The CMS contents can be viewed in the **Developer Portal** section of the admin portal for that account.
To use the `3scale-cms` command you need two pieces of information:
- Your **PROVIDER_KEY**. This can be found in the Account tab of your admin portal (only visible to the users with "admin" role).
- The **PROVIDER_DOMAIN** of your admin portal. e.g. `https://mycompany-admin.3scale.net`

### Usage

The command should be invoked from the root directory of the folder where your local copy of the CMS will be made and worked on.

Invoke the `3scale-cms` command with the following:

```bash
podman run --rm -it -v .:/cms:Z ghcr.io/fwmotion/3scale-cms PROVIDER_KEY PROVIDER_DOMAIN <action> [parameter]
```

The `3scale-cms` command has five actions:
- **info**      - show information about contents of the CMS and the local files. It accepts the optional parameter: 'details'
- **diff**      - show the difference in contents between the CMS and the local files. It accepts the optional parameter: 'details'
- **download**  - download all the contents of the CMS (no parameter). Or specify a file or section (with its contents) to download
- **upload**    - upload all the local files (no parameter). Or specify a file or section (with its contents) to upload
- **delete**    - delete all (that can be deleted) or a specific entry in the remote CMS

Usage of each is explained below:

### cms info
Show information about contents of the CMS and the local files. It accepts the optional parameter: 'details'

```bash
podman run --rm -it -v .:/cms:Z ghcr.io/fwmotion/3scale-cms PROVIDER_KEY PROVIDER_DOMAIN info
```

Output should resemble:

```
Contacting CMS at PROVIDER_DOMAIN/admin/api/cms to get content list
The layout 'main_layout' in file '/l_main_layout.html.liquid' was selected as the default layout for uploading new pages
118 items found in CMS
7 ignored local files (matching patterns in '.cmsignore')
152 (non-ignored) local files
8 implicit folders due to file/template system_names containing '/'
```

Use

```bash
podman run --rm -it -v .:/cms:Z ghcr.io/fwmotion/3scale-cms PROVIDER_KEY PROVIDER_DOMAIN info details
```

to get the list of specific files in each of those four categories:
- CMS contents elements
- Locally ignored files
- Local files that are not being ignored
- List of folders created due to CMS elements with '/' in the name

### 3scale-cms diff
Shows the differences in contents (taking into account ignored files and implicit folders) between the CMS and the local
files.

Use

```bash
podman run --rm -it -v .:/cms:Z ghcr.io/fwmotion/3scale-cms PROVIDER_KEY PROVIDER_DOMAIN diff
```

Output should resemble:
```
Contacting CMS at PROVIDER_DOMAIN/admin/api/cms to get content list
The layout 'main_layout' was selected as the default layout for uploading new pages

Summary:
0 files to be created locally
0 files to be updated locally
17 files to be created on CMS
1 files to be updated on CMS
```

to get the list of specific files to be applied on 'download' and 'upload' use:

```bash
podman run --rm -it -v .:/cms:Z ghcr.io/fwmotion/3scale-cms PROVIDER_KEY PROVIDER_DOMAIN diff details
```

### 3scale-cms download
If used without an additional file/directory name parameter, this action downloads the entire contents of the CMS that either doesn't exist locally, or is out of date locally (based on timestamps of files/folders).

If a filename is specified, then only that file is downloaded (if it exists in the CMS and is out of date locally).

If a directory name is specified, then it and all its contents (recursively down) are checked and any content that is found to exist in the CMS and is out of date is downloaded.

Files matching patterns in `.cmsignore` and skipped.

### 3scale-cms upload
If used without an additional file/directory name parameter, this action uploads all local files found under the current working directory that are either out of date in the CMS (based on timestamps) or do not exist in the CMS.

If a filename is specified, then only that file is uploaded (if it exists in the CMS and is out of date, or does not exist in the CMS).

If a directory name is specified, then it and all its contents (recursively down) are checked and any content that is found to not exist in the CMS or is out of date in the CMS is uploadd.

Files matching patterns in `.cmsignore` and skipped.

### 3scale-cms delete
If used without an additional parameter this will attempt to delete all content under the 'root' section on the remote CMS (indicated via domain parameter).

If used with a specific filename it will attempt to delete that entry int he remote CMS.

If used with a foldername, it will attempt to delete that section and all sections and content under it in the CMS.

NOTE: This action cannot be undone, and should be used with caution. Double check the domain parameter you intend to use.

## Workflow

### Getting started
Create a directory where you will work on your CMS locally

    cd ~
    mkdir my_cms
    cd my_cms

### Create your `.cmsignore` file

    touch .cmsignore

You can edit this file at any time.

### Check the status

```bash
podman run --rm -it -v .:/cms:Z ghcr.io/fwmotion/3scale-cms PROVIDER_KEY PROVIDER_DOMAIN info details
```

### Download the current contents of your CMS

```bash
podman run --rm -it -v .:/cms:Z ghcr.io/fwmotion/3scale-cms PROVIDER_KEY PROVIDER_DOMAIN download
```

#!/usr/bin/env ruby

# SEMPRE depends on several library/data files into |lib|.  Run this script to
# copy those dependencies to your local directory.  This allows you to run
# SEMPRE from anywhere.  This file consists of a set of modules (which loosely
# correspond to the code modules).
#
# The master copy of these dependencies are stored on the Stanford NLP machines.
#
# Usage:
#   ./pull-dependencies <module-1> ... <module-n>
#
# For developers with ssh access to NLP machines, there are two more local commands:
# - Copy or link |sourcePath| into lib/|dir|.
#   ./pull-dependencies -l <module-1> ... <module-n>
# - Deploy the dependencies to the NLP machines's public www.
#   ./pull-dependencies -l -r <module-1> ... <module-n>

# Specify the version of the dependencies
# (To developer: Update this before releasing a new version!)
$version = '2.0'

$isLocal = ARGV.index('-l')
$isRelease = ARGV.index('-r')
if $isRelease and not $isLocal
  puts "ERROR: To release, must use both -l and -r"
  exit 1
end
ARGV.delete_if { |x| x == '-l' or x == '-r' }

def isZip(name)
  # Directories are zipped
  name.end_with?('.exec') or name !~ /\./
end

def pull(sourcePath, dir=nil, opts={})
  puts sourcePath
  destDir = 'lib' + (dir ? '/' + dir : '')
  system "mkdir -p #{destDir}"

  name = File.basename(sourcePath)
  ext = isZip(name) ? '.zip' : ''

  if not $isLocal and not $isRelease
    # Download url => localPath
    if sourcePath.start_with?('http://') || sourcePath.start_with?('https://')
      url = sourcePath
    else
      url = 'http://nlp.stanford.edu/software/sempre/dependencies-' + $version + sourcePath + ext
    end
    localPath = destDir + '/' + name + ext
    system "mkdir -p #{File.dirname(localPath)}" or exit 1
    system "wget -nv -c '#{url}' -O #{localPath}" or exit 1
    # Unzip localPath to destDir if it's a zip file
    if isZip(name)
      system "cd #{File.dirname(localPath)} && unzip -un #{File.basename(localPath)}" or exit 1
      system "rm #{localPath}" or exit 1
    end
  else
    rsyncOpts = '-rlptDzi'  # Preserve everything except groups and permissions
    if $isRelease
      # Copy sourcePath to cluster
      baseDeployPath = '/u/apache/htdocs/static/software/sempre/dependencies-' + $version
      deployPath = baseDeployPath + sourcePath + ext
      system "mkdir -p #{File.dirname(deployPath)}" or exit 1
      if File.exists?(sourcePath)
        if isZip(name)
          system "cd #{File.dirname(sourcePath)} && zip -r #{deployPath} #{File.basename(sourcePath)}" or exit 1
        else
          if opts[:symlink]
            system "ln -sf #{File.expand_path(sourcePath)} #{deployPath}" or exit 1
          else
            system "rsync #{rsyncOpts} #{sourcePath} #{deployPath}" or exit 1
          end
        end
      else
        system "rsync #{rsyncOpts} jamie.stanford.edu:#{sourcePath} #{deployPath}" or exit 1
      end
      system "chmod -R og=u #{baseDeployPath}" #or exit 1
    else
      # Download sourcePath from cluster to destDir
      if File.exists?(sourcePath)
        if opts[:symlink]
          system "ln -sf #{File.expand_path(sourcePath)} #{destDir}" or exit 1
        else
          system "rsync #{rsyncOpts} #{sourcePath} #{destDir}" or exit 1
        end
      else
        system "rsync #{rsyncOpts} jamie.stanford.edu:#{sourcePath} #{destDir}" or exit 1
      end
    end
  end
end

# source: path to master git repository
def updateGit(source)
  dir = File.basename(source.sub(/\.git$/, ''))
  if File.exists?(dir)
    system 'cd '+dir+' && git pull' or exit 1
  else
    system 'git clone ' + source or exit 1
  end
end

def downloadExec(path)
  ['options.map', 'params', 'grammar'].each { |file|
    if File.exists?(path+'/'+file)
      pull(path+'/'+file, 'models/'+File.basename(path))
    end
  }
end

$modules = []
def addModule(name, description, func)
  $modules << [name, description, func]
end

############################################################

addModule('core', 'Core utilities (need to compile)', lambda {
  # fig: options parsing, experiment management, utils
  updateGit('https://github.com/percyliang/fig')
  system 'make -C fig' or exit 1
  system 'mkdir -p lib && cd lib && ln -sf ../fig/fig.jar' or exit 1

  # Google libraries
  pull('/u/nlp/data/semparse/resources/guava-14.0.1.jar')

  # TestNG -- testing framework
  pull('/u/nlp/data/semparse/resources/testng-6.8.5.jar')
  pull('/u/nlp/data/semparse/resources/jcommander-1.30.jar')

  # Checkstyle: make sure code looks fine
  pull('/u/nlp/data/semparse/resources/checkstyle')

  # JSON
  pull('/u/nlp/data/semparse/resources/jackson-core-2.2.0.jar')
  pull('/u/nlp/data/semparse/resources/jackson-annotations-2.2.0.jar')
  pull('/u/nlp/data/semparse/resources/jackson-databind-2.2.0.jar')

  # jLine from maven central
  pull('http://central.maven.org/maven2/jline/jline/2.14.2/jline-2.14.2.jar')

})

addModule('roboy', 'Roboy extra utilities (need to compile)', lambda {
  updateGit('https://github.com/percyliang/fig')
  system 'make -C fig' or exit 1
  system 'mkdir -p lib && cd lib && ln -sf ../fig/fig.jar' or exit 1

  # Freebase schema
  pull('/u/nlp/data/semparse/scr/freebase/state/execs/93.exec/schema2.ttl', 'fb_data/93.exec')

  # Freebase data (for lexicon)
  pull('/u/nlp/data/semparse/scr/fb_data/7', 'fb_data')

  # WebQuestions dataset
  pull('/u/nlp/data/semparse/webquestions/dataset_11/webquestions.examples.train.json', 'data/webquestions/dataset_11')
  pull('/u/nlp/data/semparse/webquestions/dataset_11/webquestions.examples.test.json', 'data/webquestions/dataset_11')

})

addModule('freebase', 'Freebase: need to construct Freebase schemas', lambda {
  # Freebase schema
  pull('/u/nlp/data/semparse/scr/freebase/state/execs/93.exec/schema2.ttl', 'fb_data/93.exec')

  # Lucene libraries
  pull('/u/nlp/data/semparse/resources/lucene-core-4.4.0.jar')
  pull('/u/nlp/data/semparse/resources/lucene-analyzers-common-4.4.0.jar')
  pull('/u/nlp/data/semparse/resources/lucene-queryparser-4.4.0.jar')

  # Freebase data (for lexicon)
  pull('/u/nlp/data/semparse/scr/fb_data/7', 'fb_data')

  # WebQuestions dataset
  pull('/u/nlp/data/semparse/webquestions/dataset_11/webquestions.examples.train.json', 'data/webquestions/dataset_11')
  pull('/u/nlp/data/semparse/webquestions/dataset_11/webquestions.examples.test.json', 'data/webquestions/dataset_11')
})



addModule('virtuoso', 'Virtuoso: if want to run own SPARQL server locally', lambda {
  updateGit('https://github.com/openlink/virtuoso-opensource')
  # Run this command to compile:
  #system "cd virtuoso-opensource && ./autogen.sh && ./configure --prefix=$PWD/install && make && make install" or exit 1
})

addModule('fullfreebase-ttl', 'Freebase (ttl file)', lambda {
  # This is just for your reference.  It is not directly used by SEMPRE.
  pull('/u/nlp/data/semparse/scr/freebase/state/execs/93.exec/0.ttl.bz2', 'fb_data/93.exec', {:symlink => true})
})

addModule('fullfreebase-vdb', 'Freebase (Virtuoso database)', lambda {
  # Virtuoso index of 0.ttl above.  This is read (and written) by Virtuoso.
  pull('/u/nlp/data/semparse/scr/freebase/state/execs/93.exec/vdb.tar.bz2', 'fb_data/93.exec', {:symlink => true})
  # You need to unzip this yourself and move these files to the right place.
})

addModule('fullfreebase-types', 'Freebase types', lambda {
  # Map from mid (e.g., fb:m.02mjmr) to id (e.g., fb:en.barack_obama) (used to link external things like Freebase API search with our internal Freebase)
  pull('/u/nlp/data/semparse/scr/freebase/freebase-rdf-2013-06-09-00-00.canonical-id-map.gz', 'fb_data', {:symlink => true})
  # Map from id to types (used to do type inference on internal entities)
  pull('/u/nlp/data/semparse/scr/freebase/freebase-rdf-2013-06-09-00-00.canonicalized.en-types.gz', 'fb_data', {:symlink => true})
  # You need to unzip these yourself and move these files to the right place.
})

addModule('tables', 'Semantic parsing with execution on tables', lambda {
  # CSV reader
  pull('/u/nlp/data/semparse/resources/opencsv-3.0.jar')
})

addModule('tables-data', 'WikiTableQuestions dataset v1.0.2', lambda {
  # Compact version of the dataset
  pull('https://github.com/ppasupat/WikiTableQuestions/releases/download/v1.0.2/WikiTableQuestions-1.0.2-compact.zip', 'data')
  # Remove old file (for backward compatibility)
  if File.directory?('lib/data/WikiTableQuestions')
    system 'rm -rv lib/data/WikiTableQuestions' or exit 1
  end
  system "cd lib/data && unzip WikiTableQuestions-1.0.2-compact.zip" or exit 1
})

addModule('tables-data-0.5', 'WikiTableQuestions dataset v0.5 (for backward reproducibility)', lambda {
  # Compact version of the dataset
  pull('https://github.com/ppasupat/WikiTableQuestions/releases/download/v0.5/WikiTableQuestions-0.5-compact.zip', 'data')
  # Remove old file (for backward compatibility)
  if File.directory?('lib/data/WikiTableQuestions')
    system 'rm -rv lib/data/WikiTableQuestions' or exit 1
  end
  system "cd lib/data && unzip WikiTableQuestions-0.5-compact.zip" or exit 1
})

addModule('overnight', 'Creating a parser for multiple domains', lambda {
  # Geo evaluation
  pull('/u/nlp/data/semparse/overnight/geo880.db', 'data/overnight/', {:symlink => true})
  pull('/u/nlp/data/semparse/overnight/geo880/geo880-train.examples', 'data/overnight/', {:symlink => true})
  pull('/u/nlp/data/semparse/overnight/geo880/geo880-test.examples', 'data/overnight/', {:symlink => true})


  # Cache for turking
  pull('/u/nlp/data/semparse/overnight/cache/', 'data/overnight/', {:symlink => true})
  pull('/u/nlp/data/semparse/overnight/layouts/', 'data/overnight/', {:symlink => true})

  # Pull testing code
  pull('/u/nlp/data/semparse/overnight/test/', 'data/overnight/', {:symlink => true})

  # Pull geo880
  pull('/u/nlp/data/semparse/overnight/geo880/geo880.paraphrases.train.superlatives.examples', 'data/overnight/', {:symlink => true})
  pull('/u/nlp/data/semparse/overnight/geo880/geo880.paraphrases.train.superlatives2.examples', 'data/overnight/', {:symlink => true})
  pull('/u/nlp/data/semparse/overnight/geo880/geo880.lexicon', 'data/overnight/', {:symlink => true})
  pull('/u/nlp/data/semparse/overnight/geo880/geo880.predicate.dict', 'data/overnight/', {:symlink => true})

  # Pull dependencies for everything else
  domains = ['geo880', 'regex', 'publications', 'socialnetwork', 'restaurants', 'blocks', 'calendar', 'housing', 'basketball', 'recipes', 'calendarplus']
  domains.each do |domain|
    pull('/u/nlp/data/semparse/overnight/' + domain + '/' + domain + '.paraphrases.train.examples', 'data/overnight/', {:symlink => true})
    pull('/u/nlp/data/semparse/overnight/' + domain + '/' + domain + '.paraphrases.test.examples', 'data/overnight/', {:symlink => true})
    pull('/u/nlp/data/semparse/overnight/' + domain + '/' + domain + '.paraphrases.groups', 'data/overnight/', {:symlink => true})
    pull('/u/nlp/data/semparse/overnight/' + domain + '/' + domain + '.word_alignments.berkeley', 'data/overnight/', {:symlink => true})
    pull('/u/nlp/data/semparse/overnight/' + domain + '/' + domain + '.phrase_alignments', 'data/overnight/', {:symlink => true})
    pull('/u/nlp/data/semparse/overnight/' + domain + '/' + domain + '-ppdb.txt', 'data/overnight/', {:symlink => true})
  end

  # Pull the independent sets for calendar
  pull('/u/nlp/data/semparse/overnight/calendar/eval/calendar.test.turk.examples', 'data/overnight/', {:symlink => true})
})

addModule('esslli_2016', 'Data for ESSLLI 2016 semantic parsing class', lambda {
  pull('/u/nlp/data/semparse/esslli_2016', 'data/esslli_2016/', {:symlink => true})
})

addModule('geo880', 'Data, lexicon, grammars and KB for geo880', lambda {
  pull('/u/nlp/data/semparse/geo880/geo880-test.examples', 'data/geo880', {:symlink => true})
  pull('/u/nlp/data/semparse/geo880/geo880-test.preprocessed.examples', 'data/geo880', {:symlink => true})
  pull('/u/nlp/data/semparse/geo880/geo880-train.preprocessed.examples', 'data/geo880', {:symlink => true})
  pull('/u/nlp/data/semparse/geo880/geo880.grammar', 'data/geo880', {:symlink => true})
  pull('/u/nlp/data/semparse/geo880/geo880.lexicon', 'data/geo880', {:symlink => true})
  pull('/u/nlp/data/semparse/geo880/geo880.kg', 'data/geo880', {:symlink => true})
  pull('/u/nlp/data/semparse/geo880/geo880.type_hierarchy', 'data/geo880', {:symlink => true})
})
############################################################

if ARGV.size == 0
  puts "#{$0} <module-1> ... <module-n>"
  puts
  puts "Modules:"
  $modules.each { |name,description,func|
    puts "  #{name}: #{description}"
  }
  puts
  puts "Internal use (Stanford NLP only):"
  puts "  #{$0} -l <module-1> ...: Get the files from the local Stanford NLP server instead"
  puts "  #{$0} -l -r <module-1> ...: Release to the public www directory on the server"
end

$modules.each { |name,description,func|
  if ARGV.index(name)
    puts "===== Downloading #{name}: #{description}"
    func.call
  end
}
FROM ruby:2.7.1

LABEL Name=3scale-cms Version=0.0.1

# throw errors if Gemfile has been modified since Gemfile.lock
RUN bundle config --global frozen 1

WORKDIR /app
COPY . /app

COPY Gemfile Gemfile.lock ./
RUN gem build 3scale-cms.gemspec && gem install 3scale-cms

CMD ["cms"]
    
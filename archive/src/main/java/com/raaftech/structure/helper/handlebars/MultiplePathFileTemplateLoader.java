package com.raaftech.structure.helper.handlebars;

import com.github.jknack.handlebars.io.TemplateLoader;
import com.github.jknack.handlebars.io.TemplateSource;
import com.github.jknack.handlebars.io.URLTemplateSource;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * MultiplePathFileTemplateLoader is a TemplateLoader that can load
 * templates from multiple directories.
 */
public class MultiplePathFileTemplateLoader implements TemplateLoader {

    private List<String> templateRepositories = new ArrayList<>();
    String suffix;

    public MultiplePathFileTemplateLoader(String path) {
        templateRepositories.add(path);
    }

    public MultiplePathFileTemplateLoader(List<String> paths) {
        templateRepositories = paths;
    }

    @Override
    public String getPrefix() {
        return templateRepositories.get(0);
    }

    @Override
    public String getSuffix() {
        return suffix;
    }

    @Override
    public void setPrefix(String templateRepository) {
        templateRepositories.add(templateRepository);
    }

    @Override
    public void setSuffix(String suffix) {
        this.suffix = suffix;
    }

    @Override
    public String resolve(String templatePath) {
        List<String> matches = templateRepositories.stream()
                .filter(repoPath -> templatePath.startsWith(repoPath))
                .collect(Collectors.toList());
        return matches.size() > 0 ? templatePath : null;
    }

    @Override
    public TemplateSource sourceAt(String templatePath) throws IOException {
        // Check templatePath against one of the valid template repositories
        String location = resolve(templatePath);
        URL resource = this.getResource(location);
        if(resource == null || location == null) {
            throw new FileNotFoundException(templatePath);
        } else {
            return new URLTemplateSource(templatePath, resource);
        }
    }

    protected URL getResource(final String location) throws IOException {
        File file = new File(location);
        return file.exists() ? file.toURI().toURL() : null;
    }
}

package com.raaftech.structure.helper.loader;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.raaftech.structure.graph.Type;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

@JsonDeserialize(using = ProfileDeserializer.class)
public class Profile {

    protected static class Command {
        protected List<String> command = new ArrayList<>();
        protected Pattern match;
    }

    // required
    private String name;
    private String desc;
    private Type providedBy;
    private Set<Type> appliesTo;

    // names
    private Set<String> serviceNames;
    private Set<String> packageNames;

    // files
    private Set<String> logFiles;
    private Set<String> dataFiles;
    private Set<String> binaryFiles;
    private Set<String> libraryFiles;
    private Set<String> configurationFiles;

    // directories
    private Set<String> logDirectories;
    private Set<String> dataDirectories;
    private Set<String> binaryDirectories;
    private Set<String> libraryDirectories;
    private Set<String> configurationDirectories;

    //commands
    private Map<String, Command> commands;

    public Profile(String name, String desc, Type providedBy, Set<Type> appliesTo) {
        this.name = name;
        this.desc = desc;
        this.providedBy = providedBy;
        this.appliesTo = appliesTo;
    }

    public String getName() {
        return name;
    }

    public String getDesc() {
        return desc;
    }

    public Type getProvidedBy() {
        return providedBy;
    }

    public Set<Type> getAppliesTo() {
        return appliesTo;
    }

    public Set<String> getServiceNames() {
        return serviceNames;
    }

    public void setServiceNames(Set<String> serviceNames) {
        this.serviceNames = serviceNames;
    }

    public Set<String> getPackageNames() {
        return packageNames;
    }

    public void setPackageNames(Set<String> packageNames) {
        this.packageNames = packageNames;
    }

    public Set<String> getLogFiles() {
        return logFiles;
    }

    public void setLogFiles(Set<String> logFiles) {
        this.logFiles = logFiles;
    }

    public Set<String> getDataFiles() {
        return dataFiles;
    }

    public void setDataFiles(Set<String> dataFiles) {
        this.dataFiles = dataFiles;
    }

    public Set<String> getBinaryFiles() {
        return binaryFiles;
    }

    public void setBinaryFiles(Set<String> binaryFiles) {
        this.binaryFiles = binaryFiles;
    }

    public Set<String> getLibraryFiles() {
        return libraryFiles;
    }

    public void setLibraryFiles(Set<String> libraryFiles) {
        this.libraryFiles = libraryFiles;
    }

    public Set<String> getConfigurationFiles() {
        return configurationFiles;
    }

    public void setConfigurationFiles(Set<String> configurationFiles) {
        this.configurationFiles = configurationFiles;
    }

    public Set<String> getLogDirectories() {
        return logDirectories;
    }

    public void setLogDirectories(Set<String> logDirectories) {
        this.logDirectories = logDirectories;
    }

    public Set<String> getDataDirectories() {
        return dataDirectories;
    }

    public void setDataDirectories(Set<String> dataDirectories) {
        this.dataDirectories = dataDirectories;
    }

    public Set<String> getBinaryDirectories() {
        return binaryDirectories;
    }

    public void setBinaryDirectories(Set<String> binaryDirectories) {
        this.binaryDirectories = binaryDirectories;
    }

    public Set<String> getLibraryDirectories() {
        return libraryDirectories;
    }

    public void setLibraryDirectories(Set<String> libraryDirectories) {
        this.libraryDirectories = libraryDirectories;
    }

    public Set<String> getConfigurationDirectories() {
        return configurationDirectories;
    }

    public void setConfigurationDirectories(Set<String> configurationDirectories) {
        this.configurationDirectories = configurationDirectories;
    }

    public Map<String, Command> getCommands() {
        return commands;
    }

    public void setCommands(Map<String, Command> commands) {
        this.commands = commands;
    }
}

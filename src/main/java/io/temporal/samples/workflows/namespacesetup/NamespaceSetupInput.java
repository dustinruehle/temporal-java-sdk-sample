package io.temporal.samples.workflows.namespacesetup;

/** Input for the namespace setup workflow. */
public record NamespaceSetupInput(String name, String region) {}

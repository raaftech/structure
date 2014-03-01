package org.openpcf.universal.model;

/**
 * Templatable represents all objects that can return a 'template' version of
 * themselves. This method can be called by anything that requires a not-yet
 * -persisted version of the component in question. This can then be editted and
 * persisted. Effectively this makes it possible for any constructed component
 * or group of components to be used as a template for new components.
 *
 * @author Rubin Simons <rubin.simons@raaftech.com>
 * @since 2014-03-01
 * @version 0.6
 */
public interface Templatable {
    public void template();
}

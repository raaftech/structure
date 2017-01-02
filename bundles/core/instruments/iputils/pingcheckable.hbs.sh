ping {{#if count}}"-c {{count}}" {{/if}}{{#if interval}}"-i {{interval}}" {{/if}}{{#if timeout}}"-W {{timeout}}" {{/if}}{{ ipv4address.addr }}

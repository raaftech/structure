ping {{#if count}}-c {{sh count}} {{/if}}{{#if interval}}-i {{sh interval}} {{/if}}{{#if timeout}}-W {{sh timeout}} {{/if}}{{sh ipv4address.addr}}

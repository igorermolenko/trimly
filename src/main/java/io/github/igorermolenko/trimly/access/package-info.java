/**
 * Owner authentication: the owner credential, its startup policy and the security filter chain that
 * guards the owner API while leaving redirects public (feature ADR-0001).
 *
 * <p>Layers: {@code api} (security filter chain, authentication entry point), {@code app}
 * (validated configuration), {@code domain} (plain Java, no Spring imports).
 */
package io.github.igorermolenko.trimly.access;

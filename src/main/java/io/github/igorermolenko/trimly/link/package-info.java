/**
 * Write path: creating short links and generating their codes. Offers the lookup that {@code
 * redirect} uses.
 *
 * <p>Layers: {@code api} (HTTP controllers, DTOs), {@code app} (use cases, transactions), {@code
 * domain} (plain Java, no Spring or persistence imports), {@code infra} (persistence, outside
 * adapters).
 */
package io.github.igorermolenko.trimly.link;

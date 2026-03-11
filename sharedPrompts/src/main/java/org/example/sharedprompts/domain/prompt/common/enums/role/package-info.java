/**
 * Role type hierarchy: three distinct groups. Add new roles in the correct place to avoid
 * deserialization and API confusion.
 *
 * <h2>1. CoreRoleType</h2>
 * Engine/core internal role layer. Used for prompt structure and LLM behavior.
 * Registered in {@link org.example.sharedprompts.domain.prompt.common.enums.serializer.RoleTypeDeserializer}
 * so that core roles can be deserialized from API when needed.
 *
 * <h2>2. DomainRoleType</h2>
 * Metadata/persona classification only. Not part of JSON role deserialization; not in
 * RoleTypeDeserializer.ROLE_TYPE_ENUMS. Use for internal tagging or UI hints. Do not add to the deserializer.
 *
 * <h2>3. Category RoleTypes (*RoleType in category subpackages)</h2>
 * API-facing, deserializable role types (e.g. WritingRoleType, BusinessRoleType). Registered in
 * RoleTypeDeserializer.ROLE_TYPE_ENUMS. When adding a new role that the API or clients can send,
 * add it as a new category enum and register it in RoleTypeDeserializer.
 *
 * <p><b>Key-based deserialization:</b> {@link org.example.sharedprompts.domain.prompt.common.enums.role.RoleTypeInterface} extends {@link org.example.sharedprompts.domain.prompt.common.contract.StableKeyedEnum},
 * so the stable-key contract (and thus key-based deserialization) is inherited. Category enums need
 * only implement {@link org.example.sharedprompts.domain.prompt.common.enums.role.RoleTypeInterface#keyPrefix()}; do not override {@code key()} unless the default
 * ({@code keyPrefix() + "." + name()}) is not appropriate.
 */
package org.example.sharedprompts.domain.prompt.common.enums.role;

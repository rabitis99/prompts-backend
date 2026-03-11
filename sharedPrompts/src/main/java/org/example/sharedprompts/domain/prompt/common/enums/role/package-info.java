/**
 * Role type hierarchy: three distinct groups. Add new roles in the correct place to avoid
 * deserialization and API confusion.
 *
 * <h2>1. CoreRoleType</h2>
 * Engine-level roles only. Used internally for prompt structure and LLM behavior.
 * Not the same as category role enums below.
 *
 * <h2>2. DomainRoleType</h2>
 * Metadata / persona classification. Not used in JSON deserialization; not registered in
 * {@link org.example.sharedprompts.domain.prompt.common.enums.serializer.RoleTypeDeserializer}.
 * Use for internal tagging or UI hints only.
 *
 * <h2>3. Category RoleTypes (*RoleType in category subpackages)</h2>
 * API-facing roles (e.g. WritingRoleType, BusinessRoleType). Registered in
 * RoleTypeDeserializer.ROLE_TYPE_ENUMS and used when deserializing request DTOs.
 * When adding a new role that the API or clients can send, add it as a new category enum
 * and register it in RoleTypeDeserializer.
 */
package org.example.sharedprompts.domain.prompt.common.enums.role;

package com.fujitsu.eventprocessor.domain;

/**
 * Tipos de evento suportados pelo sistema.
 * <ul>
 *   <li>{@link #PRODUCTION}: apenas registrado.</li>
 *   <li>{@link #QUALITY}: registrado e gera um registro de auditoria.</li>
 * </ul>
 */
public enum EventType {
    PRODUCTION,
    QUALITY
}

package com.fujitsu.eventprocessor.handler;

import com.fujitsu.eventprocessor.domain.Event;
import com.fujitsu.eventprocessor.domain.EventType;

/**
 * Estratégia de processamento específica por tipo de evento (padrão Strategy).
 * <p>
 * Para suportar um novo tipo basta criar uma nova implementação anotada com
 * {@code @Component}; nenhuma outra classe precisa ser alterada (Open/Closed).
 */
public interface EventTypeHandler {

    /** Tipo de evento atendido por esta estratégia. */
    EventType supportedType();

    /** Executa as regras específicas do tipo após o evento ser persistido. */
    void handle(Event event);
}

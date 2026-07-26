package com.alexastudillo.geographicreference.infrastructure.persistence.postgresql;

import io.vertx.mutiny.sqlclient.Row;
import io.vertx.mutiny.sqlclient.RowSet;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.function.Function;

/**
 * Converts finite reactive PostgreSQL row sets into application return shapes.
 */
@ApplicationScoped
public class ReactiveRowSetMapper {

    public <T> T firstOrNull(final RowSet<Row> rows, final Function<Row, T> mapper) {
        final Iterator<Row> iterator = rows.iterator();
        return iterator.hasNext() ? mapper.apply(iterator.next()) : null;
    }

    public <T> List<T> toList(final RowSet<Row> rows, final Function<Row, T> mapper) {
        final List<T> result = new ArrayList<>(rows.size());
        for (Row row : rows) {
            result.add(mapper.apply(row));
        }
        return List.copyOf(result);
    }
}

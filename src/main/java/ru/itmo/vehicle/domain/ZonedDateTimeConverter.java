package ru.itmo.vehicle.domain;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;

@Converter
public class ZonedDateTimeConverter implements AttributeConverter<ZonedDateTime, Timestamp> {

    @Override
    public Timestamp convertToDatabaseColumn(ZonedDateTime attribute) {
        return attribute == null ? null : Timestamp.from(attribute.toInstant());
    }

    @Override
    public ZonedDateTime convertToEntityAttribute(Timestamp databaseValue) {
        return databaseValue == null
                ? null
                : ZonedDateTime.ofInstant(Instant.ofEpochMilli(databaseValue.getTime()), ZoneId.systemDefault());
    }
}

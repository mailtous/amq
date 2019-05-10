package com.artlongs.amq.scanner;

import java.io.FilenameFilter;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.util.List;

/**
 * {@code Builder} offers a fluent API for using {@link AnnotationDetector}.
 * Its only role is to offer a more clean API.
 *
 * @author <a href="mailto:rmuller@xiam.nl">Ronald K. Muller</a>
 * @since annotation-detector 3.1.0
 */
public interface Builder {

    /**
     * Specify the annotation types to report.
     */
    Builder forAnnotations(final Class<? extends Annotation>... a);

    /**
     * Specify the Element Types to scan. If this method is not called,
     * {@link ElementType#TYPE} is used as default.
     * <p>
     * Valid types are:
     * <ul>
     * <li>{@link ElementType#TYPE}
     * <li>{@link ElementType#METHOD}
     * <li>{@link ElementType#FIELD}
     * </ul>
     * An {@code IllegalArgumentException} is thrown if another Element Type is specified or
     * no types are specified.
     */
    Builder on(final ElementType... types);

    /**
     * Filter the scanned Class Files based on its name and the directory or jar file it is
     * stored.
     * <p>
     * If the Class File is stored as a single file in the file system the {@code File}
     * argument in {@link FilenameFilter#accept(java.io.File, String) } is the
     * absolute path to the root directory scanned.
     * <p>
     * If the Class File is stored in a jar file the {@code File} argument in
     * {@link FilenameFilter#accept(java.io.File, String)} is the absolute path of
     * the jar file.
     * <p>
     * The {@code String} argument is the full name of the ClassFile in native format,
     * including package name, like {@code eu/infomas/annotation/AnnotationDetector$1.class}.
     * <p>
     * Note that all non-Class Files are already filtered and not seen by the filter.
     *
     * @param filter The filter, never {@code null}
     */
    Builder filter(final FilenameFilter filter);

    /**
     * Report the detected annotations to the specified {@code Reporter} instance.
     * 
     * @see Reporter

     */
    void report(final Reporter reporter) throws IOException;

    /**
     * Report the detected annotations to the specified {@code ReporterFunction} instance and 
     * collect the returned values of
     * {@link Reporter }.
     * The collected values are returned as a {@code List}.
     * 
     */
    <T> List<T> collect(final Reporter<T> reporter) throws IOException;

}

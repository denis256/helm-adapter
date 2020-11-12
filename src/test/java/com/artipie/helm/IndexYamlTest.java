/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2020 artipie.com
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included
 * in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NON-INFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.artipie.helm;

import com.artipie.asto.Content;
import com.artipie.asto.Key;
import com.artipie.asto.Storage;
import com.artipie.asto.ext.PublisherAs;
import com.artipie.asto.memory.InMemoryStorage;
import com.artipie.asto.rx.RxStorageWrapper;
import com.artipie.asto.test.TestResource;
import java.util.ArrayList;
import java.util.Map;
import org.apache.commons.codec.digest.DigestUtils;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.shaded.org.yaml.snakeyaml.Yaml;

/**
 * Test case for {@link IndexYaml}.
 * @checkstyle ClassDataAbstractionCouplingCheck (500 lines)
 * @since 0.2
 */
@SuppressWarnings("unchecked")
final class IndexYamlTest {

    /**
     * Chart name.
     */
    private static final String TOMCAT = "tomcat-0.4.1.tgz";

    /**
     * Base string.
     */
    private static final String BASE = "helm";

    /**
     * Storage.
     */
    private Storage storage;

    @BeforeEach
    void init() {
        this.storage = new InMemoryStorage();
        final IndexYaml yaml = new IndexYaml(this.storage, IndexYamlTest.BASE);
        yaml.update(
            new TgzArchive(
                new PublisherAs(
                    new Content.From(new TestResource(IndexYamlTest.TOMCAT).asBytes())
                ).bytes()
                    .toCompletableFuture().join()
            )).blockingGet();
    }

    @Test
    void verifyDigestFromIndex() {
        final Map<String, Object> index = new Yaml().load(
            new PublisherAs(
                new RxStorageWrapper(this.storage)
                    .value(new Key.From("index.yaml"))
                    .blockingGet()
            ).asciiString().toCompletableFuture().join()
        );
        final Map<String, Object> entries = (Map<String, Object>) index.get("entries");
        final ArrayList<Map<String, Object>> tomcat;
        tomcat = (ArrayList<Map<String, Object>>) entries.get("tomcat");
        MatcherAssert.assertThat(
            tomcat.get(0).get("digest"),
            new IsEqual<>(
                DigestUtils.sha256Hex(new TestResource(IndexYamlTest.TOMCAT).asBytes())
            )
        );
    }
}
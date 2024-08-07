/*
 * Copyright 2023 The Sigstore Authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package dev.sigstore;

import com.google.common.collect.ImmutableList;
import com.google.common.io.Resources;
import dev.sigstore.VerificationOptions.CertificateMatcher;
import dev.sigstore.bundle.Bundle;
import dev.sigstore.encryption.signers.Signers;
import dev.sigstore.strings.StringMatcher;
import dev.sigstore.testing.CertGenerator;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.security.cert.X509Certificate;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class KeylessVerifierTest {

  @Test
  public void testVerify_noDigestInBundle() throws Exception {
    var bundleFile =
        Resources.toString(
            Resources.getResource("dev/sigstore/samples/bundles/bundle-no-digest.sigstore"),
            StandardCharsets.UTF_8);
    var artifact = Resources.getResource("dev/sigstore/samples/bundles/artifact.txt").getPath();

    var verifier = KeylessVerifier.builder().sigstorePublicDefaults().build();
    verifier.verify(
        Path.of(artifact), Bundle.from(new StringReader(bundleFile)), VerificationOptions.empty());
  }

  @Test
  public void testVerify_mismatchedSet() throws Exception {
    // a bundle file where the SET is replaced with a valid SET for another artifact
    var bundleFile =
        Resources.toString(
            Resources.getResource(
                "dev/sigstore/samples/bundles/bundle-with-mismatched-set.sigstore"),
            StandardCharsets.UTF_8);
    var artifact = Resources.getResource("dev/sigstore/samples/bundles/artifact.txt").getPath();

    var verifier = KeylessVerifier.builder().sigstorePublicDefaults().build();
    Assertions.assertThrows(
        KeylessVerificationException.class,
        () ->
            verifier.verify(
                Path.of(artifact),
                Bundle.from(new StringReader(bundleFile)),
                VerificationOptions.empty()));
  }

  @Test
  public void testVerify_errorsOnDSSEBundle() throws Exception {
    var bundleFile =
        Resources.toString(
            Resources.getResource("dev/sigstore/samples/bundles/bundle.dsse.sigstore"),
            StandardCharsets.UTF_8);
    var artifact = Resources.getResource("dev/sigstore/samples/bundles/artifact.txt").getPath();

    var verifier = KeylessVerifier.builder().sigstorePublicDefaults().build();
    var ex =
        Assertions.assertThrows(
            KeylessVerificationException.class,
            () ->
                verifier.verify(
                    Path.of(artifact),
                    Bundle.from(new StringReader(bundleFile)),
                    VerificationOptions.empty()));
    Assertions.assertEquals("Cannot verify DSSE signature based bundles", ex.getMessage());
  }

  @Test
  public void testVerify_canVerifyV01Bundle() throws Exception {
    // note that this v1 bundle contains an inclusion proof
    verifyBundle(
        "dev/sigstore/samples/bundles/artifact.txt",
        "dev/sigstore/samples/bundles/bundle.v1.sigstore");
  }

  @Test
  public void testVerify_canVerifyV02Bundle() throws Exception {
    verifyBundle(
        "dev/sigstore/samples/bundles/artifact.txt",
        "dev/sigstore/samples/bundles/bundle.v2.sigstore");
  }

  @Test
  public void testVerify_canVerifyV03Bundle() throws Exception {
    verifyBundle(
        "dev/sigstore/samples/bundles/artifact.txt",
        "dev/sigstore/samples/bundles/bundle.v3.sigstore");
  }

  public void verifyBundle(String artifactResourcePath, String bundleResourcePath)
      throws Exception {
    var artifact = Resources.getResource(artifactResourcePath).getPath();
    var bundleFile =
        Resources.toString(Resources.getResource(bundleResourcePath), StandardCharsets.UTF_8);

    var verifier = KeylessVerifier.builder().sigstorePublicDefaults().build();
    verifier.verify(
        Path.of(artifact), Bundle.from(new StringReader(bundleFile)), VerificationOptions.empty());
  }

  @Test
  public void verifyWithVerificationOptions() throws Exception {
    var bundleFile =
        Resources.toString(
            Resources.getResource("dev/sigstore/samples/bundles/bundle.sigstore"),
            StandardCharsets.UTF_8);
    var artifact = Resources.getResource("dev/sigstore/samples/bundles/artifact.txt").getPath();

    var verifier = KeylessVerifier.builder().sigstorePublicDefaults().build();
    verifier.verify(
        Path.of(artifact),
        Bundle.from(new StringReader(bundleFile)),
        VerificationOptions.builder()
            .addCertificateMatchers(
                CertificateMatcher.fulcio()
                    .subjectAlternativeName(StringMatcher.string("appu@google.com"))
                    .issuer(StringMatcher.string("https://accounts.google.com"))
                    .build())
            .build());
  }

  @Test
  public void verifyCertificateMatches_noneProvided() throws Exception {
    var verifier = KeylessVerifier.builder().sigstorePublicDefaults().build();
    var certificate =
        (X509Certificate) CertGenerator.newCert(Signers.newEcdsaSigner().getPublicKey());
    Assertions.assertDoesNotThrow(() -> verifier.checkCertificateMatchers(certificate, List.of()));
  }

  @Test
  public void verifyCertificateMatches_anyOf() throws Exception {
    var verifier = KeylessVerifier.builder().sigstorePublicDefaults().build();
    var certificate =
        (X509Certificate) CertGenerator.newCert(Signers.newEcdsaSigner().getPublicKey());
    Assertions.assertDoesNotThrow(
        () ->
            verifier.checkCertificateMatchers(
                certificate,
                ImmutableList.of(
                    CertificateMatcher.fulcio()
                        .subjectAlternativeName(StringMatcher.string("not-match"))
                        .issuer(StringMatcher.string("not-match"))
                        .build(),
                    CertificateMatcher.fulcio()
                        .subjectAlternativeName(StringMatcher.string("test@test.com"))
                        .issuer(StringMatcher.string("https://fakeaccounts.test.com"))
                        .build())));
  }

  @Test
  public void verifyCertificateMatches_noneMatch() throws Exception {
    var verifier = KeylessVerifier.builder().sigstorePublicDefaults().build();
    var certificate =
        (X509Certificate) CertGenerator.newCert(Signers.newEcdsaSigner().getPublicKey());
    var ex =
        Assertions.assertThrows(
            KeylessVerificationException.class,
            () ->
                verifier.checkCertificateMatchers(
                    certificate,
                    ImmutableList.of(
                        CertificateMatcher.fulcio()
                            .subjectAlternativeName(StringMatcher.string("not-match"))
                            .issuer(StringMatcher.string("not-match"))
                            .build(),
                        CertificateMatcher.fulcio()
                            .subjectAlternativeName(StringMatcher.string("not-match-again"))
                            .issuer(StringMatcher.string("not-match-again"))
                            .build())));
    Assertions.assertEquals(
        "No provided certificate identities matched values in certificate: [{issuer:'String: not-match',san:'String: not-match'},{issuer:'String: not-match-again',san:'String: not-match-again'}]",
        ex.getMessage());
  }
}

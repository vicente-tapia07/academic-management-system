package usach.cl.demo.controller;

import org.bson.Document;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import usach.cl.demo.service.AuthorizationService;
import usach.cl.demo.service.CertificateService;

@RestController
@RequestMapping("/api/certificates")
public class CertificateController {

    private final CertificateService certificateService;
    private final AuthorizationService authorizationService;

    public CertificateController(CertificateService certificateService,
                                 AuthorizationService authorizationService) {
        this.certificateService = certificateService;
        this.authorizationService = authorizationService;
    }

    @GetMapping("/{studentId}")
    public ResponseEntity<Document> getCertificate(@PathVariable Long studentId,
                                                   Authentication authentication) {
        authorizationService.requireStudentAccess(authentication, studentId);
        return certificateService.getCertificateByUserId(studentId)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}

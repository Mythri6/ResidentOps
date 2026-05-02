package com.residentops.service;

import com.residentops.model.Notice;
import com.residentops.model.enums.NoticeType;
import com.residentops.repository.NoticeRepository;
import com.residentops.repository.UserRepository;
import com.residentops.singleton.AuditLogger;
import com.residentops.singleton.NotificationService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
public class NoticeService {
    private final NoticeRepository noticeRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final AuditLogger auditLogger;

    public NoticeService(NoticeRepository noticeRepository, UserRepository userRepository,
                         NotificationService notificationService, AuditLogger auditLogger) {
        this.noticeRepository = noticeRepository;
        this.userRepository = userRepository;
        this.notificationService = notificationService;
        this.auditLogger = auditLogger;
    }

    @Transactional
    public Notice postNotice(Long adminId, String title, String body, NoticeType type, boolean emergency) {
        var admin = userRepository.findById(adminId).orElseThrow(() -> new RuntimeException("Admin not found"));
        Notice n = new Notice();
        n.setTitle(title); n.setBody(body); n.setNoticeType(type);
        n.setPostedBy(admin); n.setEmergency(emergency);
        Notice saved = noticeRepository.save(n);
        notificationService.notifyAll(emergency ? "EMERGENCY_BROADCAST" : "NOTICE_POSTED", title, saved.getId());
        auditLogger.log("NOTICE_POSTED", "Notice", saved.getId(), adminId, type.name());
        return saved;
    }

    public List<Notice> getAll() { return noticeRepository.findAllByOrderByCreatedAtDesc(); }
    public List<Notice> getEmergencies() { return noticeRepository.findByEmergencyTrue(); }
}

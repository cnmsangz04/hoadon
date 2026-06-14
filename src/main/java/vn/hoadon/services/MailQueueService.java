package vn.hoadon.services;

import vn.hoadon.messaging.MailJobMessage;

/**
 * Pushes mail jobs onto the SQL Server database queue.
 * Decouples producers (controllers, background threads)
 * from the mail-sending implementation.
 */
public interface MailQueueService {

    /**
     * Enqueue a mail job. Returns immediately; delivery is asynchronous.
     *
     * @param message the job payload (template key + recipient + variables)
     */
    void enqueue(MailJobMessage message);
}

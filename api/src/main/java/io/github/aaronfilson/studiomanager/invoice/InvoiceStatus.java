package io.github.aaronfilson.studiomanager.invoice;

/** Where an invoice is, from upload to confirmation. */
public enum InvoiceStatus {
  /** Waiting for the file to arrive in storage. */
  UPLOADING,
  /** A reader has it. */
  READING,
  /** Read, and the numbers add up: ready for staff to check and confirm. */
  READY,
  /** The numbers don't add up, or nobody could read it: staff fill in or fix the draft. */
  NEEDS_LOOK,
  /** Staff confirmed it, and its stock was received. */
  CONFIRMED
}

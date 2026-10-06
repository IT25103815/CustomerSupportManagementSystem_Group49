package com.group49.support.util;
import java.util.Set;
/** Existing status vocabulary. Preserve the baseline staff transition policy. */
public final class TicketRules {
 public static final Set<String> STATUSES=Set.of("OPEN","ASSIGNED","IN_PROGRESS","WAITING_FOR_CUSTOMER","ESCALATED","RESOLVED","CLOSED","CANCELLED","REOPENED");
 public static boolean canTransition(String from,String to){
  // Baseline permits authorized ticket managers to select any supported status.
  // Customer edit/cancel/upload restrictions remain separate and unchanged.
  return STATUSES.contains(from) && STATUSES.contains(to);
 }
}

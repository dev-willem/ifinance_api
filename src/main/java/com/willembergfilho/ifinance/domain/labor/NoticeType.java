package com.willembergfilho.ifinance.domain.labor;

/**
 * WORKED: notice served; the termination date is the last day worked.
 * INDEMNIFIED: employer pays the notice instead of it being worked.
 * NOT_SERVED: employee resigned without serving notice (employer may discount it, CLT art. 487 §2º).
 */
public enum NoticeType { WORKED, INDEMNIFIED, NOT_SERVED }

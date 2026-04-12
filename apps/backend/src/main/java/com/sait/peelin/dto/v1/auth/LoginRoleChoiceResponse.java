package com.sait.peelin.dto.v1.auth;

import java.util.List;

public record LoginRoleChoiceResponse(String message, List<LoginAccountChoice> choices) {}

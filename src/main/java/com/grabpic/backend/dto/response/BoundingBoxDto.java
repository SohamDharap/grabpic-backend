package com.grabpic.backend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BoundingBoxDto {
    private int x;
    private int y;
    private int width;
    private int height;
}

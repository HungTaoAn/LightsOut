package com.bluerock.lightsout.entities;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * @author DanHung
 * @version V1.0
 * @Package com.mv.adan
 * @date 9/30/22 6:48 PM
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class LightDto {
    String pieceName;
    String coordinates;
    int[] intArray;
}

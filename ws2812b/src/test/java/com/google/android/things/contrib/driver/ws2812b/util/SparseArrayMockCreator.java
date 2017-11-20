package com.google.android.things.contrib.driver.ws2812b.util;


import android.util.SparseArray;

import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.util.List;

public class SparseArrayMockCreator {

    public static SparseArray<byte[]> createMockedSparseArray() {

        @SuppressWarnings("unchecked")
        SparseArray<byte[]> sparseArray = Mockito.mock(SparseArray.class);
        final ArgumentCaptor<Integer> keyCaptor = ArgumentCaptor.forClass(Integer.class);
        final ArgumentCaptor<byte[]> valueCaptor = ArgumentCaptor.forClass(byte[].class);

        Mockito.doNothing().when(sparseArray).append(keyCaptor.capture(), valueCaptor.capture());

        Mockito.when(sparseArray.get(Mockito.anyInt())).thenAnswer(new Answer<byte[]>() {
            @Override
            public byte[] answer(InvocationOnMock invocation) throws Throwable {
                Integer key = invocation.getArgumentAt(0, Integer.class);
                List<Integer> allKeys = keyCaptor.getAllValues();
                int lastIndexOfKey = allKeys.lastIndexOf(key);
                return lastIndexOfKey != -1 ? valueCaptor.getAllValues().get(lastIndexOfKey) : null;
            }
        });

        return sparseArray;
    }
}

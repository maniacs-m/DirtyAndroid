/*
 * Copyright 2016 Victor Albertos
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package app.presentation.foundation.transformations;

import app.presentation.foundation.dialogs.Dialogs;
import app.presentation.foundation.notifications.Notifications;
import io.reactivex.Single;
import io.reactivex.observers.TestObserver;
import io.reactivex.schedulers.Schedulers;
import java.util.concurrent.CancellationException;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import static junit.framework.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public final class TransformationsBehaviourTest {
  private final static String FORMATTED_ERROR = "formatted_error",
      SUCCESS_MESSAGE = "success_message";
  @Rule public MockitoRule mockitoRule = MockitoJUnit.rule();
  @Mock ExceptionFormatter exceptionFormatter;
  @Mock Notifications notifications;
  @Mock Dialogs dialogs;
  private TransformationsBehaviour transformationsBehaviourUT;

  @Before public void init() {
    when(exceptionFormatter.format(any()))
        .thenReturn(Single.just(FORMATTED_ERROR));

    transformationsBehaviourUT = new TransformationsBehaviour(exceptionFormatter,
        notifications, dialogs, Schedulers.io(), Schedulers.io());

    transformationsBehaviourUT.setLifecycle(single -> single);
  }

  @Test public void Verify_Safely_Success() {
    TestObserver<String> observer = Single.just(SUCCESS_MESSAGE)
        .compose(transformationsBehaviourUT.safely())
        .test();

    observer.awaitTerminalEvent();

    observer.assertNoErrors();
    observer.assertValueCount(1);
    assertEquals(SUCCESS_MESSAGE, observer.values().get(0));
  }

  @Test public void Verify_Safely_Error() {
    transformationsBehaviourUT.setLifecycle(single -> Single.error(new RuntimeException()));

    TestObserver<String> observer = Single.just(SUCCESS_MESSAGE)
        .compose(transformationsBehaviourUT.safely())
        .test();

    observer.awaitTerminalEvent();
    observer.assertError(RuntimeException.class);
    observer.assertNoValues();
  }

  @Test public void Verify_Safely_CancellationException() {
    transformationsBehaviourUT.setLifecycle(single -> Single.error(new CancellationException()));

    TestObserver<String> observer = Single.just(SUCCESS_MESSAGE)
        .compose(transformationsBehaviourUT.safely())
        .test();

    observer.assertNoValues();
    observer.assertNoErrors();
  }

  @Test public void Verify_ReportOnSnackBar_Success() {
    TestObserver<String> observer = Single.just(SUCCESS_MESSAGE)
        .compose(transformationsBehaviourUT.reportOnSnackBar())
        .test();

    observer.awaitTerminalEvent();

    observer.assertNoErrors();
    observer.assertValueCount(1);
    verify(exceptionFormatter, never()).format(any());
    verify(notifications, never()).showSnackBar(any(Single.class));
    assertEquals(SUCCESS_MESSAGE, observer.values().get(0));
  }

  @Test public void Verify_ReportOnSnackBar_Error() {
    TestObserver<String> observer = Single.<String>error(new RuntimeException())
        .compose(transformationsBehaviourUT.reportOnSnackBar())
        .test();

    observer.assertNoErrors();
    observer.assertNoValues();
    verify(exceptionFormatter).format(any());
    verify(notifications).showSnackBar(any(Single.class));
  }

  @Test public void Verify_ReportOnToast_Success() {
    TestObserver<String> observer = Single.just(SUCCESS_MESSAGE)
        .compose(transformationsBehaviourUT.reportOnToast())
        .test();
    observer.awaitTerminalEvent();

    observer.assertNoErrors();
    observer.assertValueCount(1);
    verify(exceptionFormatter, never()).format(any());
    verify(notifications, never()).showToast(any(Single.class));
    assertEquals(SUCCESS_MESSAGE, observer.values().get(0));
  }

  @Test public void Verify_ReportOnToast_Error() {
    TestObserver<String> observer = Single.<String>error(new RuntimeException())
        .compose(transformationsBehaviourUT.reportOnToast())
        .test();

    observer.assertNoErrors();
    observer.assertNoValues();
    verify(exceptionFormatter).format(any());
    verify(notifications).showToast(any(Single.class));
  }

  @Test public void Verify_Loading() {
    TestObserver<String> observer = Single.just(SUCCESS_MESSAGE)
        .compose(transformationsBehaviourUT.loading())
        .test();

    observer.assertNoErrors();
    observer.assertValueCount(1);
    verify(dialogs).showLoading();
    verify(dialogs).hideLoading();

    assertEquals(SUCCESS_MESSAGE, observer.values().get(0));
  }
}
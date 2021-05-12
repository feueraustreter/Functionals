/*
 * Copyright 2021 feueraustreter
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package feueraustreter.tryfunction;

import feueraustreter.tryfunction.sample.InvalidUserException;
import feueraustreter.tryfunction.sample.User;
import feueraustreter.tryfunction.sample.UserDTO;
import lombok.RequiredArgsConstructor;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

@RequiredArgsConstructor
@SuppressWarnings("java:S3577" /* Test class name */)
public class TryExample {

    private final List<User> testUsers = Arrays.asList(
            new User("foo", true),
            new User("bar", false)
    );

    @Test
    public void filterUserTyped() {
        List<UserDTO> users = getUserTyped(testUsers);
        assertThat(users.size(), is(1));
    }

    @Test
    public void filterUser() {
        List<UserDTO> users = getUser(testUsers);
        assertThat(users.size(), is(1));
    }

    private List<UserDTO> getUserTyped(List<User> users) {
        return users.stream()
                // "Typed" Stream of Try<InvalidUserException, UserDTO>:
                .map(user -> Try.tryIt((Try.TryFunction<UserDTO, InvalidUserException>)
                        () -> mapUser(user)))

                // Filter and retain successful mappings
                .filter(Try::successful)
                .map(Try::getSuccess)

                .collect(Collectors.toList());
    }

    private List<UserDTO> getUser(List<User> users) {
        return users.stream()
                // With implicit cast (losing exception type, has RuntimeException instead)
                .map(user -> Try.tryIt(() -> mapUser(user)))

                // Filter and retain successful mappings
                .filter(Try::successful)
                .map(Try::getSuccess)

                .collect(Collectors.toList());
    }

    private UserDTO mapUser(User user) throws InvalidUserException {
        if (!user.isValid()) {
            throw new InvalidUserException();
        }

        UserDTO userDTO = new UserDTO();
        userDTO.setName(user.getName());
        return userDTO;
    }

}

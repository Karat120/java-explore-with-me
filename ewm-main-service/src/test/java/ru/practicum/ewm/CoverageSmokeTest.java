package ru.practicum.ewm;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import ru.practicum.ewm.category.AdminCategoryController;
import ru.practicum.ewm.category.Category;
import ru.practicum.ewm.category.CategoryDto;
import ru.practicum.ewm.category.CategoryMapper;
import ru.practicum.ewm.category.CategoryRepository;
import ru.practicum.ewm.category.CategoryService;
import ru.practicum.ewm.category.PublicCategoryController;
import ru.practicum.ewm.common.ConflictException;
import ru.practicum.ewm.common.ErrorHandler;
import ru.practicum.ewm.common.NotFoundException;
import ru.practicum.ewm.compilation.AdminCompilationController;
import ru.practicum.ewm.compilation.Compilation;
import ru.practicum.ewm.compilation.CompilationDto;
import ru.practicum.ewm.compilation.CompilationRepository;
import ru.practicum.ewm.compilation.CompilationService;
import ru.practicum.ewm.compilation.NewCompilationDto;
import ru.practicum.ewm.compilation.PublicCompilationController;
import ru.practicum.ewm.compilation.UpdateCompilationRequest;
import ru.practicum.ewm.config.StatsClientConfig;
import ru.practicum.ewm.event.AdminEventController;
import ru.practicum.ewm.event.Event;
import ru.practicum.ewm.event.EventFullDto;
import ru.practicum.ewm.event.EventMapper;
import ru.practicum.ewm.event.EventRepository;
import ru.practicum.ewm.event.EventService;
import ru.practicum.ewm.event.EventShortDto;
import ru.practicum.ewm.event.EventState;
import ru.practicum.ewm.event.Location;
import ru.practicum.ewm.event.NewEventDto;
import ru.practicum.ewm.event.PrivateEventController;
import ru.practicum.ewm.event.PublicEventController;
import ru.practicum.ewm.event.UpdateEventDto;
import ru.practicum.ewm.request.EventRequestStatusUpdateRequest;
import ru.practicum.ewm.request.EventRequestStatusUpdateResult;
import ru.practicum.ewm.request.ParticipationRequest;
import ru.practicum.ewm.request.ParticipationRequestDto;
import ru.practicum.ewm.request.PrivateRequestController;
import ru.practicum.ewm.request.RequestMapper;
import ru.practicum.ewm.request.RequestService;
import ru.practicum.ewm.request.RequestStatus;
import ru.practicum.ewm.user.AdminUserController;
import ru.practicum.ewm.user.User;
import ru.practicum.ewm.user.UserDto;
import ru.practicum.ewm.user.UserMapper;
import ru.practicum.ewm.user.UserRepository;
import ru.practicum.ewm.user.UserService;
import ru.practicum.stats.client.StatsClient;

@ExtendWith(MockitoExtension.class)
class CoverageSmokeTest {

    @Mock
    private UserService userService;
    @Mock
    private CategoryService categoryService;
    @Mock
    private EventService eventService;
    @Mock
    private RequestService requestService;
    @Mock
    private CompilationService compilationService;

    @Test
    void controllersShouldDelegateToServices() {
        AdminUserController userController = new AdminUserController(userService);
        UserDto userDto = UserDto.builder().id(1L).email("a@b.c").name("n").build();
        when(userService.create(any())).thenReturn(userDto);
        when(userService.getAllByIds(anyList(), anyInt(), anyInt())).thenReturn(List.of(userDto));
        assertEquals(userDto, userController.create(userDto));
        assertEquals(1, userController.getAll(List.of(1L), 0, 10).size());
        userController.delete(1L);
        verify(userService).delete(1L);

        CategoryDto categoryDto = CategoryDto.builder().id(1L).name("cat").build();
        AdminCategoryController adminCategoryController = new AdminCategoryController(categoryService);
        PublicCategoryController publicCategoryController = new PublicCategoryController(categoryService);
        when(categoryService.create(any())).thenReturn(categoryDto);
        when(categoryService.update(anyLong(), any())).thenReturn(categoryDto);
        when(categoryService.getAll(anyInt(), anyInt())).thenReturn(List.of(categoryDto));
        when(categoryService.getById(anyLong())).thenReturn(categoryDto);
        assertEquals(categoryDto, adminCategoryController.create(categoryDto));
        assertEquals(categoryDto, adminCategoryController.update(1L, categoryDto));
        adminCategoryController.delete(1L);
        assertEquals(1, publicCategoryController.getAll(0, 10).size());
        assertEquals(categoryDto, publicCategoryController.getById(1L));

        EventFullDto eventFullDto = EventFullDto.builder().id(1L).annotation("a".repeat(20)).build();
        EventShortDto eventShortDto = EventShortDto.builder().id(1L).annotation("a".repeat(20)).build();
        UpdateEventDto updateEventDto = new UpdateEventDto();
        NewEventDto newEventDto = new NewEventDto();
        newEventDto.setAnnotation("a".repeat(20));
        newEventDto.setDescription("d".repeat(20));
        newEventDto.setTitle("title");
        newEventDto.setCategory(1L);
        newEventDto.setLocation(new Location(1.0f, 1.0f));
        newEventDto.setEventDate(LocalDateTime.now().plusDays(1));

        AdminEventController adminEventController = new AdminEventController(eventService);
        PublicEventController publicEventController = new PublicEventController(eventService);
        PrivateEventController privateEventController = new PrivateEventController(eventService);
        when(eventService.searchAdmin(anyList(), anyList(), anyList(), any(), any(), anyInt(), anyInt())).thenReturn(List.of(eventFullDto));
        when(eventService.updateByAdmin(anyLong(), any())).thenReturn(eventFullDto);
        when(eventService.searchPublic(any(), any(), any(), any(), any(), anyBoolean(), any(), anyInt(), anyInt(), any()))
                .thenReturn(List.of(eventShortDto));
        when(eventService.getPublishedById(anyLong(), any())).thenReturn(eventFullDto);
        when(eventService.createByUser(anyLong(), any())).thenReturn(eventFullDto);
        when(eventService.getByUser(anyLong(), anyInt(), anyInt())).thenReturn(List.of(eventShortDto));
        when(eventService.getByUserAndId(anyLong(), anyLong())).thenReturn(eventFullDto);
        when(eventService.updateByUser(anyLong(), anyLong(), any())).thenReturn(eventFullDto);
        assertEquals(1, adminEventController.search(List.of(1L), List.of("PENDING"), List.of(1L), null, null, 0, 10).size());
        assertEquals(eventFullDto, adminEventController.update(1L, updateEventDto));
        assertEquals(1, publicEventController.search(null, null, null, null, null, false, "EVENT_DATE", 0, 10, null).size());
        assertEquals(eventFullDto, publicEventController.getById(1L, null));
        assertEquals(eventFullDto, privateEventController.create(1L, newEventDto));
        assertEquals(1, privateEventController.getAll(1L, 0, 10).size());
        assertEquals(eventFullDto, privateEventController.getById(1L, 1L));
        assertEquals(eventFullDto, privateEventController.update(1L, 1L, updateEventDto));

        PrivateRequestController privateRequestController = new PrivateRequestController(requestService);
        ParticipationRequestDto requestDto = ParticipationRequestDto.builder().id(1L).event(1L).requester(1L).status(RequestStatus.PENDING).build();
        EventRequestStatusUpdateResult result = EventRequestStatusUpdateResult.builder()
                .confirmedRequests(List.of(requestDto))
                .rejectedRequests(List.of())
                .build();
        when(requestService.getByUser(anyLong())).thenReturn(List.of(requestDto));
        when(requestService.create(anyLong(), anyLong())).thenReturn(requestDto);
        when(requestService.cancel(anyLong(), anyLong())).thenReturn(requestDto);
        when(requestService.getEventRequests(anyLong(), anyLong())).thenReturn(List.of(requestDto));
        when(requestService.updateStatuses(anyLong(), anyLong(), any())).thenReturn(result);
        assertEquals(1, privateRequestController.getByUser(1L).size());
        assertEquals(requestDto, privateRequestController.create(1L, 1L));
        assertEquals(requestDto, privateRequestController.cancel(1L, 1L));
        assertEquals(1, privateRequestController.getEventRequests(1L, 1L).size());
        assertEquals(result, privateRequestController.updateStatuses(1L, 1L, new EventRequestStatusUpdateRequest()));

        AdminCompilationController adminCompilationController = new AdminCompilationController(compilationService);
        PublicCompilationController publicCompilationController = new PublicCompilationController(compilationService);
        CompilationDto compilationDto = CompilationDto.builder().id(1L).title("c").pinned(false).events(List.of()).build();
        when(compilationService.create(any())).thenReturn(compilationDto);
        when(compilationService.update(anyLong(), any())).thenReturn(compilationDto);
        when(compilationService.getAll(any(), anyInt(), anyInt())).thenReturn(List.of(compilationDto));
        when(compilationService.getById(anyLong())).thenReturn(compilationDto);
        assertEquals(compilationDto, adminCompilationController.create(new NewCompilationDto()));
        adminCompilationController.delete(1L);
        assertEquals(compilationDto, adminCompilationController.update(1L, new UpdateCompilationRequest()));
        assertEquals(1, publicCompilationController.getAll(null, 0, 10).size());
        assertEquals(compilationDto, publicCompilationController.getById(1L));
    }

    @Test
    void mappersShouldMapAllMainDtos() {
        UserMapper userMapper = new UserMapper();
        User user = User.builder().id(1L).email("a@b.c").name("name").build();
        assertEquals("name", userMapper.toDto(user).getName());
        assertEquals("a@b.c", userMapper.toEntity(userMapper.toDto(user)).getEmail());
        assertEquals(1L, userMapper.toShortDto(user).getId());

        CategoryMapper categoryMapper = new CategoryMapper();
        Category category = Category.builder().id(2L).name("cat").build();
        assertEquals("cat", categoryMapper.toDto(category).getName());
        assertEquals(2L, categoryMapper.toEntity(categoryMapper.toDto(category)).getId());

        Event event = Event.builder()
                .id(3L)
                .annotation("annotation")
                .category(category)
                .description("description")
                .eventDate(LocalDateTime.now().plusDays(2))
                .initiator(user)
                .paid(false)
                .participantLimit(0)
                .requestModeration(true)
                .location(new Location(1.0f, 1.0f))
                .title("title")
                .state(EventState.PENDING)
                .createdOn(LocalDateTime.now())
                .build();
        EventMapper eventMapper = new EventMapper(categoryMapper, userMapper);
        assertEquals(3L, eventMapper.toShortDto(event, 1L, 2L).getId());
        assertEquals("description", eventMapper.toFullDto(event, 1L, 2L).getDescription());

        ParticipationRequest request = ParticipationRequest.builder()
                .id(4L)
                .event(event)
                .requester(user)
                .status(RequestStatus.PENDING)
                .created(LocalDateTime.now())
                .build();
        RequestMapper requestMapper = new RequestMapper();
        assertEquals(4L, requestMapper.toDto(request).getId());
    }

    @Test
    void coreServicesAndCommonClassesShouldBeCovered() {
        UserRepository userRepository = org.mockito.Mockito.mock(UserRepository.class);
        UserMapper userMapper = org.mockito.Mockito.spy(new UserMapper());
        UserService testedUserService = new UserService(userRepository, userMapper);
        UserDto dto = UserDto.builder().email("x@y.z").name("nm").build();
        User saved = User.builder().id(10L).email("x@y.z").name("nm").build();
        when(userRepository.save(any())).thenReturn(saved);
        when(userRepository.findAll(any(org.springframework.data.domain.Pageable.class)))
                .thenReturn(new org.springframework.data.domain.PageImpl<>(List.of(saved)));
        when(userRepository.findByIdIn(anyList())).thenReturn(List.of(saved));
        when(userRepository.existsById(10L)).thenReturn(true);
        when(userRepository.findById(10L)).thenReturn(Optional.of(saved));
        assertEquals(10L, testedUserService.create(dto).getId());
        assertEquals(1, testedUserService.getAll(0, 10).size());
        assertEquals(1, testedUserService.getAllByIds(List.of(10L), 0, 1).size());
        testedUserService.delete(10L);
        assertEquals(10L, testedUserService.getByIdOrThrow(10L).getId());
        when(userRepository.existsById(11L)).thenReturn(false);
        assertThrows(NotFoundException.class, () -> testedUserService.delete(11L));

        CategoryRepository categoryRepository = org.mockito.Mockito.mock(CategoryRepository.class);
        CategoryMapper categoryMapper = new CategoryMapper();
        CategoryService testedCategoryService = new CategoryService(categoryRepository, categoryMapper);
        Category cat = Category.builder().id(20L).name("cat").build();
        when(categoryRepository.save(any())).thenReturn(cat);
        when(categoryRepository.findById(20L)).thenReturn(Optional.of(cat));
        when(categoryRepository.existsById(20L)).thenReturn(true);
        when(categoryRepository.findAll(any(org.springframework.data.domain.Pageable.class)))
                .thenReturn(new org.springframework.data.domain.PageImpl<>(List.of(cat)));
        assertEquals("cat", testedCategoryService.create(CategoryDto.builder().name("cat").build()).getName());
        assertEquals(20L, testedCategoryService.update(20L, CategoryDto.builder().name("cat2").build()).getId());
        testedCategoryService.delete(20L);
        assertEquals(20L, testedCategoryService.getByIdOrThrow(20L).getId());
        assertEquals(1, testedCategoryService.getAll(0, 10).size());
        when(categoryRepository.existsById(30L)).thenReturn(false);
        assertThrows(NotFoundException.class, () -> testedCategoryService.delete(30L));

        CompilationRepository compilationRepository = org.mockito.Mockito.mock(CompilationRepository.class);
        EventRepository eventRepository = org.mockito.Mockito.mock(EventRepository.class);
        EventMapper eventMapper = org.mockito.Mockito.mock(EventMapper.class);
        CompilationService testedCompilationService = new CompilationService(compilationRepository, eventRepository, eventMapper);
        Compilation comp = new Compilation();
        comp.setId(1L);
        comp.setTitle("comp");
        comp.setPinned(false);
        Event compilationEvent = Event.builder()
                .id(99L)
                .annotation("ann")
                .description("desc")
                .title("title")
                .eventDate(LocalDateTime.now().plusDays(1))
                .createdOn(LocalDateTime.now())
                .state(EventState.PENDING)
                .category(cat)
                .initiator(saved)
                .location(new Location(1.0f, 1.0f))
                .build();
        comp.setEvents(Set.of(compilationEvent));
        when(compilationRepository.save(any())).thenReturn(comp);
        when(compilationRepository.existsById(1L)).thenReturn(true);
        when(compilationRepository.findById(1L)).thenReturn(Optional.of(comp));
        when(compilationRepository.findAll(any(org.springframework.data.domain.Pageable.class)))
                .thenReturn(new org.springframework.data.domain.PageImpl<>(List.of(comp)));
        when(compilationRepository.findByPinned(anyBoolean(), any(org.springframework.data.domain.Pageable.class)))
                .thenReturn(new org.springframework.data.domain.PageImpl<>(List.of(comp)));
        when(eventRepository.findAllById(anySet())).thenReturn(List.of(compilationEvent));
        when(eventMapper.toShortDto(any(), anyLong(), anyLong())).thenReturn(EventShortDto.builder().id(1L).build());
        NewCompilationDto newCompilationDto = new NewCompilationDto();
        newCompilationDto.setTitle("comp");
        newCompilationDto.setEvents(Set.of(99L));
        assertEquals(1L, testedCompilationService.create(newCompilationDto).getId());
        testedCompilationService.delete(1L);
        assertEquals(1L, testedCompilationService.update(1L, new UpdateCompilationRequest()).getId());
        assertEquals(1, testedCompilationService.getAll(null, 0, 10).size());
        assertEquals(1, testedCompilationService.getAll(false, 0, 10).size());
        assertEquals(1L, testedCompilationService.getById(1L).getId());

        ErrorHandler errorHandler = new ErrorHandler();
        assertEquals("404 NOT_FOUND", errorHandler.handleNotFound(new NotFoundException("nf")).getStatus());
        assertEquals("409 CONFLICT", errorHandler.handleConflict(new ConflictException("c")).getStatus());
        assertEquals("409 CONFLICT", errorHandler.handleIntegrity(new DataIntegrityViolationException("d")).getStatus());
        assertEquals("400 BAD_REQUEST", errorHandler.handleBadRequest(new IllegalArgumentException("b")).getStatus());
        assertEquals("400 BAD_REQUEST", errorHandler.handleUnreadable(new HttpMessageNotReadableException("u")).getStatus());

        StatsClientConfig config = new StatsClientConfig();
        StatsClient client = config.statsClient("http://localhost:9090");
        assertNotNull(client);
        assertEquals("x", new NotFoundException("x").getMessage());
    }
}

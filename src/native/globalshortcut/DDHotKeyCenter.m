/*
 DDHotKey -- DDHotKeyCenter.m
 
 Copyright (c) 2010, Dave DeLong <http://www.davedelong.com>
 
 Permission to use, copy, modify, and/or distribute this software for any purpose with or without fee is hereby granted, provided that the above copyright notice and this permission notice appear in all copies.
 
 The software is  provided "as is", without warranty of any kind, including all implied warranties of merchantability and fitness. In no event shall the author(s) or copyright holder(s) be liable for any claim, damages, or other liability, whether in an action of contract, tort, or otherwise, arising from, out of, or in connection with the software or the use or other dealings in the software.
 */

#import "DDHotKeyCenter.h"
#import <Carbon/Carbon.h>
#import <objc/runtime.h>

#pragma mark Private Global Declarations

static NSMutableSet * _registeredHotKeys = nil;
static UInt32 _nextHotKeyID = 1;
OSStatus dd_hotKeyHandler(EventHandlerCallRef nextHandler, EventRef theEvent, void * userData);
UInt32 dd_translateModifierFlags(NSUInteger flags);
NSString* dd_stringifyModifierFlags(NSUInteger flags);

#pragma mark DDHotKey

@implementation DDHotKey

- (id) target { return nil; }
- (SEL) action { return nil; }
- (id) object { return nil; }
- (unsigned short) keyCode { return 0; }
- (NSUInteger) modifierFlags { return 0; }

#if NS_BLOCKS_AVAILABLE
- (DDHotKeyTask) task { return nil; }
#endif

- (NSUInteger) hash {
	return [self keyCode] + [self modifierFlags];
}

- (BOOL) isEqual:(id)object {
	BOOL equal = NO;
	if ([object isKindOfClass:[DDHotKey class]]) {
		equal = ([object keyCode] == [self keyCode]);
		equal &= ([object modifierFlags] == [self modifierFlags]);
	}
	return equal;
}

- (NSString *) description {
	NSString * flags = dd_stringifyModifierFlags([self modifierFlags]);
	NSString * invokes = @"(block)";
	if ([self target] != nil && [self action] != nil) {
		invokes = [NSString stringWithFormat:@"[%@ %@]", [self target], NSStringFromSelector([self action])];
	}
	return [NSString stringWithFormat:@"%@\n\t(key: %hu\n\tflags: %@\n\tinvokes: %@)", [super description], [self keyCode], flags, invokes];
}

@end

@interface _DDHotKey : DDHotKey {
	@private
	id target;
	SEL action;
	id object;
	
#if NS_BLOCKS_AVAILABLE
	DDHotKeyTask task;
#endif
	
	unsigned short keyCode;
	NSUInteger modifierFlags;
	UInt32 hotKeyID;
	NSValue * hotKeyRef;
}

@property (nonatomic, retain) id target;
@property (nonatomic) SEL action;
@property (nonatomic, retain) id object;
@property (nonatomic) unsigned short keyCode;
@property (nonatomic) NSUInteger modifierFlags;
@property (nonatomic) UInt32 hotKeyID;
@property (nonatomic, retain) NSValue * hotKeyRef;

#if NS_BLOCKS_AVAILABLE
@property (nonatomic, copy) DDHotKeyTask task;
#endif

- (void) invokeWithEvent:(NSEvent *)event;
- (BOOL) registerHotKey;
- (void) unregisterHotKey;

@end

@implementation _DDHotKey

@synthesize target, action, object, keyCode, modifierFlags, hotKeyID, hotKeyRef;
#if NS_BLOCKS_AVAILABLE
@synthesize task;
#endif

- (Class) class { return [DDHotKey class]; }

- (void) invokeWithEvent:(NSEvent *)event {
	if (target != nil && action != nil && [target respondsToSelector:action]) {
		[target performSelector:action withObject:event withObject:object];
	}
#if NS_BLOCKS_AVAILABLE
	else if (task != nil) {
		task(event);
	}
#endif
}

- (NSString *) actionString {
	return NSStringFromSelector(action);
}

- (BOOL) registerHotKey {
	EventHotKeyID keyID;
	keyID.signature = 'htk1';
	keyID.id = _nextHotKeyID;
	
	EventHotKeyRef carbonHotKey;
	UInt32 flags = dd_translateModifierFlags(modifierFlags);
#ifdef __LP64__
    OSStatus err = RegisterEventHotKey(keyCode, flags, keyID, GetEventDispatcherTarget(), 0, &carbonHotKey);
#else
    OSStatus err = RegisterEventHotKey(keyCode, flags, keyID, GetApplicationEventTarget(), 0, &carbonHotKey);
#endif
    	
	//error registering hot key
	if (err != 0) { return NO; }
	
	NSValue * refValue = [NSValue valueWithPointer:carbonHotKey];
	[self setHotKeyRef:refValue];
	[self setHotKeyID:_nextHotKeyID];
	
	_nextHotKeyID++;
	
	return YES;
}

- (void) unregisterHotKey {
	EventHotKeyRef carbonHotKey = (EventHotKeyRef)[hotKeyRef pointerValue];
	UnregisterEventHotKey(carbonHotKey);
	[self setHotKeyRef:nil];
}

- (void) dealloc {
	[target release], target = nil;
	[object release], object = nil;
	if (hotKeyRef != nil) {
		[self unregisterHotKey];
		[hotKeyRef release], hotKeyRef = nil;
	}
	[super dealloc];
}

@end

#pragma mark DDHotKeyCenter

@implementation DDHotKeyCenter

+ (void) initialize {
	if (self == [DDHotKeyCenter class] && _registeredHotKeys == nil) {
		_registeredHotKeys = [[NSMutableSet alloc] init];
		_nextHotKeyID = 1;
		EventTypeSpec eventSpec;
		eventSpec.eventClass = kEventClassKeyboard;
		eventSpec.eventKind = kEventHotKeyReleased;
		InstallApplicationEventHandler(&dd_hotKeyHandler, 1, &eventSpec, NULL, NULL);
	}
}

- (NSSet *) hotKeysMatchingPredicate:(NSPredicate *)predicate {
	return [_registeredHotKeys filteredSetUsingPredicate:predicate];
}

- (BOOL) hasRegisteredHotKeyWithKeyCode:(unsigned short)keyCode modifierFlags:(NSUInteger)flags {
	NSPredicate * predicate = [NSPredicate predicateWithFormat:@"keyCode = %hu AND modifierFlags = %lu", keyCode, flags];
	return ([[self hotKeysMatchingPredicate:predicate] count] > 0);
}

#if NS_BLOCKS_AVAILABLE
- (BOOL) registerHotKeyWithKeyCode:(unsigned short)keyCode modifierFlags:(NSUInteger)flags task:(DDHotKeyTask)task {
	//we can't add a new hotkey if something already has this combo
	if ([self hasRegisteredHotKeyWithKeyCode:keyCode modifierFlags:flags]) { return NO; }
	
	_DDHotKey * newHotKey = [[_DDHotKey alloc] init];
	[newHotKey setTask:task];
	[newHotKey setKeyCode:keyCode];
	[newHotKey setModifierFlags:flags];
	
	BOOL success = [newHotKey registerHotKey];
	if (success) {
		[_registeredHotKeys addObject:newHotKey];
	}
	
	[newHotKey release];
	return success;
}
#endif

- (BOOL) registerHotKeyWithKeyCode:(unsigned short)keyCode modifierFlags:(NSUInteger)flags target:(id)target action:(SEL)action object:(id)object {
	//we can't add a new hotkey if something already has this combo
	if ([self hasRegisteredHotKeyWithKeyCode:keyCode modifierFlags:flags]) { return NO; }
	
	//build the hotkey object:
	_DDHotKey * newHotKey = [[_DDHotKey alloc] init];
	[newHotKey setTarget:target];
	[newHotKey setAction:action];
	[newHotKey setObject:object];
	[newHotKey setKeyCode:keyCode];
	[newHotKey setModifierFlags:flags];
	
	BOOL success = [newHotKey registerHotKey];
	if (success) {
		[_registeredHotKeys addObject:newHotKey];
	}
	
	[newHotKey release];
	return success;
}

- (void) unregisterHotKeysMatchingPredicate:(NSPredicate *)predicate {
	//explicitly unregister the hotkey, since relying on the unregistration in -dealloc can be problematic
	NSAutoreleasePool * pool = [[NSAutoreleasePool alloc] init];
	NSSet * matches = [self hotKeysMatchingPredicate:predicate];
	[_registeredHotKeys minusSet:matches];
	[matches makeObjectsPerformSelector:@selector(unregisterHotKey)];
	[pool release];
}

- (void) unregisterHotKey:(DDHotKey *)hotKey {
	if (object_getClass(hotKey) == [_DDHotKey class]) {
		_DDHotKey * key = (_DDHotKey *)hotKey;
		[_registeredHotKeys removeObject:key];
		[key unregisterHotKey];
	} else {
		[NSException raise:NSInvalidArgumentException format:@"Invalid hotkey"];
	}
}

- (void) unregisterHotKeysWithTarget:(id)target {
	NSPredicate * predicate = [NSPredicate predicateWithFormat:@"target = %@", target];
	[self unregisterHotKeysMatchingPredicate:predicate];
}

- (void) unregisterHotKeysWithTarget:(id)target action:(SEL)action {
	NSPredicate * predicate = [NSPredicate predicateWithFormat:@"target = %@ AND actionString = %@", target, NSStringFromSelector(action)];
	[self unregisterHotKeysMatchingPredicate:predicate];
}

- (void) unregisterHotKeyWithKeyCode:(unsigned short)keyCode modifierFlags:(NSUInteger)flags {
	NSPredicate * predicate = [NSPredicate predicateWithFormat:@"keyCode = %hu AND modifierFlags = %lu", keyCode, flags];
	[self unregisterHotKeysMatchingPredicate:predicate];
}

- (NSSet *) registeredHotKeys {
	return [self hotKeysMatchingPredicate:[NSPredicate predicateWithFormat:@"hotKeyRef != NULL"]];
}

@end

OSStatus dd_hotKeyHandler(EventHandlerCallRef nextHandler, EventRef theEvent, void * userData) {
	NSAutoreleasePool * pool = [[NSAutoreleasePool alloc] init];
	
	EventHotKeyID hotKeyID;
	GetEventParameter(theEvent, kEventParamDirectObject, typeEventHotKeyID, NULL, sizeof(hotKeyID),NULL,&hotKeyID);
	
	UInt32 keyID = hotKeyID.id;
	
	NSSet * matchingHotKeys = [_registeredHotKeys filteredSetUsingPredicate:[NSPredicate predicateWithFormat:@"hotKeyID = %u", keyID]];
	if ([matchingHotKeys count] > 1) { NSLog(@"ERROR!"); }
	_DDHotKey * matchingHotKey = [matchingHotKeys anyObject];
	
	NSEvent * event = [NSEvent eventWithEventRef:theEvent];
	NSEvent * keyEvent = [NSEvent keyEventWithType:NSKeyUp 
										  location:[event locationInWindow] 
									 modifierFlags:[event modifierFlags]
										 timestamp:[event timestamp] 
									  windowNumber:-1 
										   context:nil 
										characters:@"" 
					   charactersIgnoringModifiers:@"" 
										 isARepeat:NO 
										   keyCode:[matchingHotKey keyCode]];

	[matchingHotKey invokeWithEvent:keyEvent];
	
	[pool release];
	
	return noErr;
}

UInt32 dd_translateModifierFlags(NSUInteger flags) {
	UInt32 newFlags = 0;
	if ((flags & NSControlKeyMask) > 0) { newFlags |= controlKey; }
	if ((flags & NSCommandKeyMask) > 0) { newFlags |= cmdKey; }
	if ((flags & NSShiftKeyMask) > 0) { newFlags |= shiftKey; }
	if ((flags & NSAlternateKeyMask) > 0) { newFlags |= optionKey; }
	return newFlags;
}

NSString* dd_stringifyModifierFlags(NSUInteger flags) {
	NSMutableArray * bits = [NSMutableArray array];
	if ((flags & NSControlKeyMask) > 0) { [bits addObject:@"NSControlKeyMask"]; }
	if ((flags & NSCommandKeyMask) > 0) { [bits addObject:@"NSCommandKeyMask"]; }
	if ((flags & NSShiftKeyMask) > 0) { [bits addObject:@"NSShiftKeyMask"]; }
	if ((flags & NSAlternateKeyMask) > 0) { [bits addObject:@"NSAlternateKeyMask"]; }
	if ([bits count] > 0) {
		return [NSString stringWithFormat:@"(%@)", [bits componentsJoinedByString:@" | "]];
	}
	return @"ERROR: No valid flags";
}

import { isApiError } from '@studiomanager/api-client';
import type { ReactNode } from 'react';
import {
  ActivityIndicator,
  Pressable,
  StyleSheet,
  Text,
  TextInput,
  type TextInputProps,
  useColorScheme,
  View,
} from 'react-native';

export interface Palette {
  background: string;
  surface: string;
  text: string;
  muted: string;
  accent: string;
  onAccent: string;
  warning: string;
  border: string;
}

const light: Palette = {
  background: '#f7f4ef',
  surface: '#ffffff',
  text: '#1f1a17',
  muted: '#6b625b',
  accent: '#9a4a2c',
  onAccent: '#ffffff',
  warning: '#a3570a',
  border: '#ddd5cc',
};

const dark: Palette = {
  background: '#171412',
  surface: '#221e1b',
  text: '#f2ece6',
  muted: '#b0a69d',
  accent: '#e08a63',
  onAccent: '#1f1a17',
  warning: '#f0a35c',
  border: '#3a332e',
};

export function usePalette(): Palette {
  return useColorScheme() === 'dark' ? dark : light;
}

export function Screen({ children }: { children: ReactNode }) {
  const palette = usePalette();
  return <View style={[styles.screen, { backgroundColor: palette.background }]}>{children}</View>;
}

export function Title({ children }: { children: ReactNode }) {
  const palette = usePalette();
  return (
    <Text accessibilityRole="header" style={[styles.title, { color: palette.text }]}>
      {children}
    </Text>
  );
}

export function Body({ children, muted = false }: { children: ReactNode; muted?: boolean }) {
  const palette = usePalette();
  return <Text style={[styles.body, { color: muted ? palette.muted : palette.text }]}>{children}</Text>;
}

export function Badge({ children, warning = false }: { children: ReactNode; warning?: boolean }) {
  const palette = usePalette();
  return (
    <Text style={[styles.badge, { color: warning ? palette.warning : palette.muted, borderColor: palette.border }]}>
      {children}
    </Text>
  );
}

export function Button({
  label,
  onPress,
  primary = false,
  disabled = false,
}: {
  label: string;
  onPress: () => void;
  primary?: boolean;
  disabled?: boolean;
}) {
  const palette = usePalette();
  return (
    <Pressable
      accessibilityRole="button"
      accessibilityState={{ disabled }}
      disabled={disabled}
      onPress={onPress}
      style={({ pressed }) => [
        styles.button,
        {
          backgroundColor: primary ? palette.accent : palette.surface,
          borderColor: primary ? palette.accent : palette.border,
          opacity: disabled ? 0.5 : pressed ? 0.8 : 1,
        },
      ]}
    >
      <Text style={[styles.buttonText, { color: primary ? palette.onAccent : palette.text }]}>{label}</Text>
    </Pressable>
  );
}

export function Field({ label, ...input }: { label: string } & TextInputProps) {
  const palette = usePalette();
  return (
    <View style={styles.field}>
      <Text style={[styles.label, { color: palette.muted }]}>{label}</Text>
      <TextInput
        accessibilityLabel={label}
        placeholderTextColor={palette.muted}
        style={[styles.input, { color: palette.text, borderColor: palette.border, backgroundColor: palette.surface }]}
        {...input}
      />
    </View>
  );
}

export function Card({ children }: { children: ReactNode }) {
  const palette = usePalette();
  return <View style={[styles.card, { backgroundColor: palette.surface, borderColor: palette.border }]}>{children}</View>;
}

export function Loading() {
  const palette = usePalette();
  return <ActivityIndicator accessibilityLabel="Loading" color={palette.accent} style={styles.loading} />;
}

/** Shows the API's problem detail when there is one, so the person sees what to fix. */
export function ErrorText({ error }: { error: unknown }) {
  const palette = usePalette();
  const message = isApiError(error)
    ? error.message
    : error instanceof Error
      ? error.message
      : 'Something went wrong. Check your connection and try again.';
  return (
    <Text accessibilityRole="alert" style={[styles.body, { color: palette.warning }]}>
      {message}
    </Text>
  );
}

const styles = StyleSheet.create({
  screen: { flex: 1, padding: 16, gap: 12 },
  title: { fontSize: 24, fontWeight: '700' },
  body: { fontSize: 16, lineHeight: 22 },
  badge: { fontSize: 13, borderWidth: 1, borderRadius: 999, paddingHorizontal: 8, paddingVertical: 2, alignSelf: 'flex-start' },
  button: { borderWidth: 1, borderRadius: 10, paddingVertical: 12, paddingHorizontal: 16, alignItems: 'center' },
  buttonText: { fontSize: 16, fontWeight: '600' },
  field: { gap: 4, flexGrow: 1 },
  label: { fontSize: 14 },
  input: { borderWidth: 1, borderRadius: 8, paddingHorizontal: 12, paddingVertical: 10, fontSize: 16 },
  card: { borderWidth: 1, borderRadius: 12, padding: 12, gap: 6 },
  loading: { marginTop: 24 },
});

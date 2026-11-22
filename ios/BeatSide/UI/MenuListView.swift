import SwiftUI

private let rowHeight: CGFloat = 34

/// The rows of a submenu. The highlighted row is kept fully visible: `scrollTo` with a nil
/// anchor scrolls the minimum amount needed (FR-016).
struct MenuListView: View {
    let children: [MenuNode]
    let selectedIndex: Int
    let settings: WheelSettings

    var body: some View {
        ScrollViewReader { proxy in
            ScrollView(.vertical) {
                VStack(spacing: 0) {
                    ForEach(Array(children.enumerated()), id: \.offset) { index, child in
                        MenuRow(child: child, selected: index == selectedIndex, settings: settings)
                            .id(index)
                    }
                }
            }
            .scrollDisabled(true)
            .scrollIndicators(.hidden)
            .onAppear { proxy.scrollTo(selectedIndex) }
            .onChange(of: selectedIndex) { _, newIndex in
                proxy.scrollTo(newIndex)
            }
        }
    }
}

private struct MenuRow: View {
    let child: MenuNode
    let selected: Bool
    let settings: WheelSettings

    private var suffix: String {
        switch child {
        case .submenu, .screen:
            return "›"
        case .toggle(_, let key):
            return settings.isOn(key) ? String(localized: "setting_on") : String(localized: "setting_off")
        }
    }

    var body: some View {
        HStack {
            Text(MenuStrings.title(for: child))
                .lineLimit(1)
                .truncationMode(.tail)
            Spacer(minLength: 8)
            Text(suffix)
        }
        .font(.system(size: 17, weight: .semibold))
        .foregroundStyle(selected ? LcdColors.selectedText : LcdColors.text)
        .padding(.horizontal, 10)
        .frame(height: rowHeight)
        .frame(maxWidth: .infinity)
        .background { if selected { LcdColors.selection } }
        // FR-028: one screen-reader element with the name plus the role or state.
        .accessibilityElement(children: .ignore)
        .accessibilityLabel(MenuAccessibility.rowLabel(child, settings: settings, strings: CatalogAccessibilityStrings()))
    }
}
